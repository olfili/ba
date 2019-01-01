package olga.ba;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.*;

import org.apache.uima.UIMAException;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class PageNavigator {
	
	private WebDriver driver;
	
	private ArrayList<String> tagNamesToIgnore;
	
	public PageNavigator() {
		System.setProperty("webdriver.chrome.driver", "resources/chromedriver.exe");   
    	
    	ChromeOptions options = new ChromeOptions();
    	options.addExtensions(new File ("resources/adblock-plus-crx-master/adblock-plus-crx-master/bin/Adblock-Plus_v1.12.4.crx"));
    	    	
    	this.driver = new ChromeDriver(options);
    	
    	this.driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
    	
    	this.fillIgnoredTagNames();
	}
	
	private void fillIgnoredTagNames() {
		tagNamesToIgnore = new ArrayList<String>();
		tagNamesToIgnore.add("img");
		tagNamesToIgnore.add("abbr");
		tagNamesToIgnore.add("i");
		tagNamesToIgnore.add("input");
		tagNamesToIgnore.add("select");
		tagNamesToIgnore.add("button");
		tagNamesToIgnore.add("script");
		tagNamesToIgnore.add("noscript");
	}
	
	public void navigate(String url) throws UIMAException {
		driver.get(url);
        handlePopup();
        handleAds();
        String text = this.getText(this.getTextRoot(url));
        
        System.out.println("Found longest text:\n" + text);
        //System.out.println("Tokens:\n" + TokenizedWriterPipeline.tokenize(text));
        
        driver.close();
	}
	
	private String getTextRoot(String url) {
		return ".articleContent>.clearfix";
	}
	
	private String _test(String cssPath) {
		if(cssPath.isEmpty()) {
			cssPath = ".articleContent>.clearfix";
		}
        WebElement textTag = driver.findElement(By.cssSelector(cssPath));
        
        System.out.println("Title: " + driver.getTitle());
        System.out.println("Total Text:\n" + textTag.getText());
        return textTag.getText();
	}
	
	private void handlePopup() {
    	List<WebElement> popups = driver.findElements(By.cssSelector(".cleverpush-confirm"));
    	for(WebElement popup: popups) {
    		popup.findElement(By.cssSelector(".cleverpush-confirm-btn-deny")).click();
    	}
    }
    
    private void handleAds() {
    	String cssPath = ".ialayerContainer";
    	By locator = By.cssSelector(cssPath);
//    	WebDriverWait wait = new WebDriverWait(driver, 30);
//        wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    	List<WebElement> ads = driver.findElements(locator);
    	for(WebElement ad:ads) {
    		removeElement(ad);
    	}
    }
    
    private void removeElement(WebElement element) {
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

	public String getText(String cssPathToRootElement) {
		WebElement root = driver.findElement(By.cssSelector(cssPathToRootElement));
		return getLongestElementContent(root);
	}
	
	public String getLongestElementContent(WebElement element) {
		if(this.tagNamesToIgnore.contains(element.getTagName())) {
			return "";
		}
		List<WebElement> children = element.findElements(By.xpath(".//*"));
		if(children.isEmpty()) {
			return element.getText();
		}
		String longestText = "";
		for(int i =0; i < children.size(); i++) {
			WebElement child = children.get(i);
			System.out.println(child.getTagName() + " " + child.getAttribute("class"));
			String content = getLongestElementContent(child);
			if(longestText.length() < content.length()) {
				longestText = content;
			}
		}
		return longestText;
	}
}