package browsers

import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import org.openqa.selenium.{Cookie, WebElement}
import org.scalatest.funspec.AnyFunSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.test.TestServer

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import play.api.Configuration
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.Mode
import scala.concurrent.ExecutionContext
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit

object Base {
  val PORT = 9001

  lazy val app: Application = new GuiceApplicationBuilder()
    .configure(Configuration.from(Map.empty))
    .in(Mode.Test)
    .build()

  lazy val testServer: TestServer = {
    val s = TestServer(
      port = PORT,
      application = app
    )
    s.start()

    s
  }
}

trait Base 
  extends AnyFunSpecLike
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with Matchers {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  def await[T](future: Future[T]): T = Await.result(future, Duration.apply(5, TimeUnit.MINUTES))

  lazy val webDriver: ChromeDriver = {

    def init(retryCount: Int = 4): ChromeDriver = {
      val options = new ChromeOptions()
      // TIP: Comment the below line to see the browser in the headful mode.
      // Or, in the SBT console, you can set no-headless to true with `set Test / javaOptions += "-Dno-headless=false"`.
      // Add Thread.sleep(10000) in your test in order to see the current state of the browser.
      if (Option(sys.props("no-headless")).contains("true")) {
        // Show the browser
      } else {
        // options.addArguments("--headless")
      }

      options.addArguments("--disable-extensions")
      options.addArguments("--disable-gpu")
      options.addArguments("--disable-web-security")
      options.addArguments("--window-size=800,640")
      options.addArguments("--disable-dev-shm-usage")
      options.addArguments("--disable-smooth-scrolling")

      try {
        val driver = new ChromeDriver(options)

        driver
      } catch {
        case e: Exception =>
          Thread.sleep(250)
          if (retryCount > 0) {
            init(retryCount - 1)
          } else {
            throw e
          }
      }
    }

    init()
  }

  override protected def beforeAll(): Unit = {
    val _testServer = Base.testServer // init the test server

    super.beforeAll()
    webDriver.getWindowHandle() // initialize web driver
  }

  override protected def afterAll(): Unit = {
    webDriver.quit()
    super.afterAll()
  }
}