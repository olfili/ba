package olga.ba;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
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
	
	private HashMap<WebElement, ElementType> markedElements;
	
	public PageNavigator() {
		System.setProperty("webdriver.chrome.driver", "resources/chromedriver.exe");   
    	
    	ChromeOptions options = new ChromeOptions();
    	options.addExtensions(new File ("resources/adblock-plus-crx-master/adblock-plus-crx-master/bin/Adblock-Plus_v1.12.4.crx"));
    	    	
    	this.driver = new ChromeDriver(options);
    	
    	this.driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
    	
    	this.markedElements = new HashMap<WebElement, PageNavigator.ElementType>();
    	
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
	
	public String getElementText(WebElement element) {
		String script = "var parent = arguments[0];" + 
				"var child = parent.firstChild;" + 
				"var ret = '';" + 
				"while(child) {" + 
				"    if (child.nodeType === Node.TEXT_NODE)" + 
				"        ret += child.textContent;" + 
				"    child = child.nextSibling;" + 
				"}" + 
				"return ret;";
		String result = (String)this.executeScript(script, element);
		return result;
	}
	
	public Object executeScript(String script, Object parameter) {
		JavascriptExecutor executor;
    	if(driver instanceof JavascriptExecutor) {
    		executor = (JavascriptExecutor) driver;
    		Object result = executor.executeScript(script, parameter);
    		return result;
    	}
    	return null;
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

	public String getText(String cssPathToRootElement) throws UIMAException {
		//WebElement root = driver.findElement(By.cssSelector(cssPathToRootElement));
		this.analyseElements();
		StringBuilder result = new StringBuilder();
		for(WebElement element: this.markedElements.keySet()) {
			ElementType type = this.markedElements.get(element); 
			if(type == ElementType.RelevantText 
					|| type == ElementType.HeaderText) {
				result.append(this.getElementText(element));
				result.append("\n");
			}
		}
		return result.toString();
	}
	
	private void analyseElements() throws UIMAException {
		 this.markedElements = new HashMap<WebElement, ElementType>();
		// Check Elements with the following TagNames: div, p, h1, h2, h3, h4, text 
		// <a ...> is a part of text
		List<WebElement> elements = driver.findElements(By.xpath("//div|//td|//p|//h1|//h2|//h3|//h4"));
		for(WebElement element : elements) {
			System.out.println("CURR: " + element.getTagName() + " " + element.getAttribute("class") + " " + element.getAttribute("id"));
			analyseElement(element); // fills <this.markedElements> 
			System.out.println("---------------------------------------------------------------------------------");
		}
	}
	
	private void analyseElement(WebElement element) throws UIMAException {
		List<WebElement> precedingElements = element.findElements(By.xpath("preceding-sibling::*"));
		List<WebElement> followingElements = element.findElements(By.xpath("following-sibling::*"));
		if(precedingElements.size() > 0 && followingElements.size() > 0) {
			ElementType type = ElementType.IrrelevantInformation;
			WebElement prev = element.findElement(By.xpath("preceding-sibling::*"));
			System.out.println("PREV: " + prev.getTagName() + " " + prev.getAttribute("class") + " " + prev.getAttribute("id"));
			double prevWordCount = this.elementWordCount(prev);
			double prevLinkDensity = this.elementLinkDensity(prev);;
			
			double curWordCount = this.elementWordCount(element);
			double curLinkDensity = this.elementLinkDensity(element);
			WebElement next = element.findElement(By.xpath("following-sibling::*"));
			System.out.println("NEXT: " + next.getTagName() + " " + next.getAttribute("class") + " " + next.getAttribute("id"));
			double nextWordCount = this.elementWordCount(next);
			//Not used in the current algorithm:
			//double nextLinkDensity = this.elementLinkDensity(next);;
			//TODO: add the link to the article for the numbers
			if(curLinkDensity <= 0.333333) {
				if(prevLinkDensity <= 0.555556) {
					if(curWordCount <= 16) {
						if(nextWordCount <= 15) {
							if(prevWordCount <= 4) {
								type = ElementType.IrrelevantInformation;
							} else {
								type = ElementType.RelevantText;
							}
						} else {
							type = ElementType.RelevantText;
						}
					} else {
						type = ElementType.RelevantText;
					}
				} else {
					if(curWordCount <= 40) {
						if(nextWordCount <= 17) {
							type = ElementType.IrrelevantInformation;
						} else {
							type = ElementType.RelevantText;
						}
					} else {
						type = ElementType.RelevantText;
					}
				} 
			} else {
				type = ElementType.IrrelevantInformation; 
			}
			this.markedElements.put(element, type);
		}
	}
	
	public double elementWordCount(WebElement element) throws UIMAException {
		String text = this.getElementText(element);
		List<String> words = TextTokenizer.tokenize(text);
		return (double) words.size();
	}
	
	public double elementLinkDensity(WebElement element) {
		List<WebElement> links = element.findElements(By.tagName("a"));
		List<WebElement> allSubElements = element.findElements(By.xpath("//*"));
		return (double)links.size() / allSubElements.size();
	}
	
	private enum ElementType {
		RelevantText,
		HeaderText,
		IrrelevantInformation
	}
}
