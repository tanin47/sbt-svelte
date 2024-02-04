package browsers

import org.scalatest.SequentialNestedSuiteExecution

class BrowserSpec extends Base with SequentialNestedSuiteExecution {
  it("clicks on a button") {
    webDriver.get(s"http://localhost:${Base.PORT}")

    webDriver.findElementByCssSelector("[data-test-id='text']").getText() should be("Value: off")

    webDriver.findElementByCssSelector(".our-button").click()
    webDriver.findElementByCssSelector("[data-test-id='text']").getText() should be("Value: on")

    webDriver.findElementByCssSelector(".our-js-button").click()
    webDriver.findElementByCssSelector("[data-test-id='text']").getText() should be("Value: off")
  }
}
