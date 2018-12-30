package olga.ba;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class App {
    public static void main( String[] args ) throws InterruptedException {
    	String URL = "http://www.news.de/promis/855737575/kate-middleton-prinz-william-meghan-markle-und-prinz-harry-das-waren-schoensten-momente-der-royals-2018/1/";
    	System.setProperty("webdriver.chrome.driver", "resources/chromedriver.exe");       
    	WebDriver driver = new ChromeDriver();
    	driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
        driver.get(URL);
        handlePopup(driver);
        handleAds(driver);
        String cssPath = ".articleContent>.clearfix";
        WebElement textTag = driver.findElement(By.cssSelector(cssPath));
        
        System.out.println("Title: " + driver.getTitle());
        System.out.println("Total Text:\n" + textTag.getText());
        driver.close();
    }
    
    private static void handlePopup(WebDriver driver) {
    	List<WebElement> popups = driver.findElements(By.cssSelector(".cleverpush-confirm"));
    	for(WebElement popup: popups) {
    		popup.findElement(By.cssSelector(".cleverpush-confirm-btn-deny")).click();
    	}
    }
    
    private static void handleAds(WebDriver driver) {
    	String cssPath = ".ialayerContainer";
    	By locator = By.cssSelector(cssPath);
//    	WebDriverWait wait = new WebDriverWait(driver, 30);
//        wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    	List<WebElement> ads = driver.findElements(locator);
    	for(WebElement ad:ads) {
    		removeElement(driver, ad);
    	}
    }
    
    private static void removeElement(WebDriver driver, WebElement element) {
    	JavascriptExecutor executor;
    	if(driver instanceof JavascriptExecutor) {
    		executor = (JavascriptExecutor) driver;
    		String className = element.getAttribute("class");
    		executor.executeScript("var els = document.getElementsByClassName('" + className + "');"
    				+ "for(var i = 0; i < els.length; i++) {"
    				+ "	els[i].remove(); "
    				+ "}");
    	}
    }
}
