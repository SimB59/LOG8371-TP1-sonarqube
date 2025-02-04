package org.sonar.api.impl.ws;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class NewFunctionTest {
 NewFunction f = new NewFunction();
 @Test
 public void testHelloWorld() {
     assertThat(f.HelloWorld()).isEqualTo("Hello World");
 }
}
