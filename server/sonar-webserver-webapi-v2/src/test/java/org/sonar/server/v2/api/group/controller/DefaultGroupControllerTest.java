/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.v2.api.group.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.GroupDto;
import org.sonar.server.common.group.service.GroupService;
import org.sonar.server.common.management.ManagedInstanceChecker;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.v2.api.ControllerTester;
import org.sonar.server.v2.api.group.response.RestGroupResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.v2.WebApiEndpoints.GROUPS_ENDPOINT;
import static org.sonar.server.v2.WebApiEndpoints.JSON_MERGE_PATCH_CONTENT_TYPE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class DefaultGroupControllerTest {

  private static final String GROUP_UUID = "1234";
  private static final Gson GSON = new GsonBuilder().create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final GroupService groupService = mock();
  private final DbClient dbClient = mock();
  private final DbSession dbSession = mock();
  private final ManagedInstanceChecker managedInstanceChecker = mock();
  private final MockMvc mockMvc = ControllerTester.getMockMvc(new DefaultGroupController(groupService, dbClient, managedInstanceChecker, userSession));

  @Before
  public void setUp() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
  }

  @Test
  public void fetchGroup_whenGroupExists_returnsTheGroup() throws Exception {

    GroupDto groupDto = new GroupDto().setUuid(GROUP_UUID).setName("name").setDescription("description");

    when(groupService.findGroupByUuid(dbSession, GROUP_UUID)).thenReturn(Optional.of(groupDto));

    userSession.logIn().setSystemAdministrator();
    mockMvc.perform(get(GROUPS_ENDPOINT + "/" + GROUP_UUID))
      .andExpectAll(
        status().isOk(),
        content().json("""
          {
            "id": "1234",
            "name": "name",
            "description": "description"
          }
          """));
  }

  @Test
  public void fetchGroup_whenCallerIsNotAdmin_shouldReturnForbidden() throws Exception {
    userSession.logIn().setNonSystemAdministrator();
    mockMvc.perform(
        get(GROUPS_ENDPOINT + "/" + GROUP_UUID))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));
  }

  @Test
  public void fetchGroup_whenGroupDoesntExist_shouldReturnNotFound() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(groupService.findGroupByUuid(dbSession, GROUP_UUID)).thenReturn(Optional.empty());
    mockMvc.perform(
        get(GROUPS_ENDPOINT + "/" + GROUP_UUID).content("{}"))
      .andExpectAll(
        status().isNotFound(),
        content().json("{\"message\":\"Group '1234' not found\"}"));
  }

  @Test
  public void deleteGroup_whenCallerIsNotAdmin_shouldReturnForbidden() throws Exception {
    userSession.logIn().setNonSystemAdministrator();
    mockMvc.perform(
        delete(GROUPS_ENDPOINT + "/" + GROUP_UUID))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));
  }

  @Test
  public void deleteGroup_whenInstanceIsManaged_shouldReturnException() throws Exception {
    userSession.logIn().setSystemAdministrator();
    doThrow(BadRequestException.create("the instance is managed")).when(managedInstanceChecker).throwIfInstanceIsManaged();
    mockMvc.perform(
        delete(GROUPS_ENDPOINT + "/" + GROUP_UUID))
      .andExpectAll(
        status().isBadRequest(),
        content().json("{\"message\":\"the instance is managed\"}"));
  }

  @Test
  public void deleteGroup_whenGroupDoesntExist_shouldReturnNotFound() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(groupService.findGroupByUuid(dbSession, GROUP_UUID)).thenReturn(Optional.empty());
    mockMvc.perform(
        delete(GROUPS_ENDPOINT + "/" + GROUP_UUID).content("{}"))
      .andExpectAll(
        status().isNotFound(),
        content().json("{\"message\":\"Group '1234' not found\"}"));
  }

  @Test
  public void deleteGroup_whenGroupExists_shouldDeleteAndReturn204() throws Exception {
    GroupDto groupDto = new GroupDto().setUuid(GROUP_UUID).setName("name").setDescription("description");

    when(groupService.findGroupByUuid(dbSession, GROUP_UUID)).thenReturn(Optional.of(groupDto));

    userSession.logIn().setSystemAdministrator();
    mockMvc.perform(
        delete(GROUPS_ENDPOINT + "/" + GROUP_UUID))
      .andExpectAll(
        status().isNoContent(),
        content().string(""));
  }

  @Test
  public void patchGroup_whenCallerIsNotAdmin_shouldReturnForbidden() throws Exception {
    userSession.logIn().setNonSystemAdministrator();
    mockMvc.perform(
        patch(GROUPS_ENDPOINT + "/" + GROUP_UUID).contentType(JSON_MERGE_PATCH_CONTENT_TYPE).content("{}")
      )
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));
  }

  @Test
  public void patchGroup_whenInstanceIsManaged_shouldReturnException() throws Exception {
    userSession.logIn().setSystemAdministrator();
    doThrow(BadRequestException.create("the instance is managed")).when(managedInstanceChecker).throwIfInstanceIsManaged();
    mockMvc.perform(
        patch(GROUPS_ENDPOINT + "/" + GROUP_UUID).contentType(JSON_MERGE_PATCH_CONTENT_TYPE).content("{}")
      )
      .andExpectAll(
        status().isBadRequest(),
        content().json("{\"message\":\"the instance is managed\"}"));
  }

  @Test
  public void patchGroup_whenGroupDoesntExist_shouldReturnNotFound() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(groupService.findGroupByUuid(dbSession, GROUP_UUID)).thenReturn(Optional.empty());
    mockMvc.perform(
        patch(GROUPS_ENDPOINT + "/" + GROUP_UUID).contentType(JSON_MERGE_PATCH_CONTENT_TYPE).content("{}")
      )
      .andExpectAll(
        status().isNotFound(),
        content().json("{\"message\":\"Group '1234' not found\"}"));
  }

  @Test
  public void patchGroup_whenGroupExists_shouldPatchAndReturnNewGroup() throws Exception {
    patchGroupAndAssertResponse("newName", "newDescription");
  }

  @Test
  public void patchGroup_whenGroupExistsAndRemovingDescription_shouldPatchAndReturnNewGroup() throws Exception {
    patchGroupAndAssertResponse("newName", null);
  }

  @Test
  public void patchGroup_whenGroupExistsAndIdempotent_shouldPatch() throws Exception {
    patchGroupAndAssertResponse("newName", "newDescription");
    patchGroupAndAssertResponse("newName", "newDescription");
  }

  private void patchGroupAndAssertResponse(@Nullable String newName,@Nullable String newDescription) throws Exception {
    userSession.logIn().setSystemAdministrator();
    GroupDto groupDto = new GroupDto().setUuid(GROUP_UUID).setName("name").setDescription("description");
    when(groupService.findGroupByUuid(dbSession, GROUP_UUID)).thenReturn(Optional.of(groupDto));
    GroupDto newDto = new GroupDto().setUuid(GROUP_UUID).setName(newName).setDescription(newDescription);
    when(groupService.updateGroup(dbSession, groupDto, newName, newDescription)).thenReturn(newDto);

    MvcResult mvcResult = mockMvc.perform(
        patch(GROUPS_ENDPOINT + "/" + GROUP_UUID).contentType(JSON_MERGE_PATCH_CONTENT_TYPE).content(
          """
            {
              "name": "%s",
              "description": %s
            }
            """.formatted(newName, newDescription == null ? "null" : "\"" + newDescription + "\"")
        )
      )
      .andExpect(status().isOk())
      .andReturn();

    RestGroupResponse restGroupResponse = GSON.fromJson(mvcResult.getResponse().getContentAsString(), RestGroupResponse.class);
    assertThat(restGroupResponse.id()).isEqualTo(GROUP_UUID);
    assertThat(restGroupResponse.name()).isEqualTo(newName);
    assertThat(restGroupResponse.description()).isEqualTo(newDescription);
  }

}
