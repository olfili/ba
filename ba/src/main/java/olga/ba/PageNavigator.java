package olga.ba;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.*;

import org.apache.uima.UIMAException;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class PageNavigator {
	
	private WebDriver driver;
	
	private ArrayList<String> relevantTagNames;
	
	//private HashMap<WebElement, ElementType> markedElements;
	
	private ArrayList<WebElement> textElements;
	
	public PageNavigator() {
		System.setProperty("webdriver.chrome.driver", "resources/chromedriver.exe");   
    	
    	ChromeOptions options = new ChromeOptions();
    	options.addExtensions(new File ("resources/adblock-plus-crx-master/adblock-plus-crx-master/bin/Adblock-Plus_v1.12.4.crx"));
    	    	
    	this.driver = new ChromeDriver(options);
    	
    	this.driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
    	
//    	this.markedElements = new HashMap<WebElement, PageNavigator.ElementType>();
    	this.textElements = new ArrayList<WebElement>();
    	
    	this.fillRelevantTags();
	}
	
	private void fillRelevantTags() {
		relevantTagNames = new ArrayList<String>();
		relevantTagNames.add("div");
		relevantTagNames.add("p");
		relevantTagNames.add("h1");
		relevantTagNames.add("h2");
		relevantTagNames.add("h3");
		relevantTagNames.add("h4");
	}
	
	public String getElementText(WebElement element) {
		String script = "var parent = arguments[0];" + 
				"var child = parent.firstChild;" + 
				"var ret = '';" + 
				"while(child) {" + 
				"    if (child.nodeType === Node.TEXT_NODE "
				+ "			|| child.tagName === 'A'"
				+ "			|| child.tagName === 'STRONG'"
				+ "			|| child.tagName === 'EM'"
				+ "			|| child.tagName === 'SPAN'"
				+ ") {"  
				+ "      ret += child.textContent;"
				+ "		 console.log(child.tagName + ' ' + child.textContent);"
				+ "	 }" + 
				"    child = child.nextSibling;" + 
				"}" + 
				"return ret;";
		String result = (String)this.executeScript(script, element);
		return result;
	}
	
	public Object executeScript(String script, Object parameters) {
		JavascriptExecutor executor;
    	if(driver instanceof JavascriptExecutor) {
    		executor = (JavascriptExecutor) driver;
    		Object result = executor.executeScript(script, parameters);
    		return result;
    	}
    	return null;
	} 
	
	public void navigate(String url) throws UIMAException {
		driver.get(url);
        handlePopup();
        handleAds();
        String text = this.getText();
        
        //System.out.println("Found longest text:\n" + text);
        this.write_file(text, "PageText");
        //System.out.println("Tokens:\n" + TokenizedWriterPipeline.tokenize(text));
        
        driver.close();
	}
	
	public void write_file(String text, String name) {
		FileWriter fw = null;
		try {
			fw = new FileWriter(name + ".txt");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	    BufferedWriter bw = new BufferedWriter(fw);
	    try {
			bw.write(text);
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
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

	public String getText() throws UIMAException {
		this.analyseElements();
		StringBuilder result = new StringBuilder();
		for(WebElement element: this.textElements) {
			result.append(this.getElementText(element).trim());
			//result.append(element.getText());
			result.append("\n");
		}
		return result.toString();
	}
	
	private void analyseElements() throws UIMAException {
//		 this.markedElements = new HashMap<WebElement, ElementType>();
		this.textElements = new ArrayList<WebElement>();
		// Check Elements with the following TagNames: div, p, h1, h2, h3, h4, text 
		// <a ...> is a part of text
		List<WebElement> elements = driver.findElements(By.cssSelector("body *"));
		elements = filterElements(elements);
		for(int i = 1; i < elements.size() - 1; i++) {
			WebElement prev = elements.get(i-1);
			if(!this.relevantTagNames.contains(prev.getTagName())) {
				continue;
			}
			WebElement element = elements.get(i);
			WebElement next = elements.get(i+1);
			
			System.out.println("CURR: " + element.getTagName() + " " + element.getAttribute("class") + " " + element.getAttribute("id"));
			try {
				analyseElement(prev, element, next); // fills <this.markedElements>
			} catch (StaleElementReferenceException e) {
				// most probably will fail on taking one of the <next> elements,
				// so remove <next> and try again with another <next>
				elements.remove(i+1);
				i--;
			}
			System.out.println("---------------------------------------------------------------------------------");
		}
	}
	
	private List<WebElement> filterElements(List<WebElement> elements) {
		for(int i = 0; i < elements.size(); i++) {
			try {
				WebElement el = elements.get(i);
				if(!this.relevantTagNames.contains(el.getTagName())) {
					elements.remove(i);
					i--;
				}
			} catch (StaleElementReferenceException e) {
				elements.remove(i);
				i--;
			}
		}
		return elements;
	}
	
	// TODO: replace HashMap with an ArrayList, since we don't need the irrelevantInformation at all
	// 			and the headers are relevant anyway
	private void analyseElement(WebElement prev, WebElement element, WebElement next) throws UIMAException {
//			ElementType type = ElementType.IrrelevantInformation;
			
			System.out.println("PREV: " + prev.getTagName() + " " + prev.getAttribute("class") + " " + prev.getAttribute("id"));
			double prevWordCount = this.elementWordCount(prev);
			System.out.println("PREV WordCount: " + prevWordCount);
			double prevLinkDensity = this.elementLinkDensity(prev);
			System.out.println("PREV LinkDensity: " + prevLinkDensity);
			
			double curWordCount = this.elementWordCount(element);
			System.out.println("CURR WordCount: " + curWordCount);
			double curLinkDensity = this.elementLinkDensity(element);
			System.out.println("CURR LinkDensity: " + curLinkDensity);
			
			System.out.println("NEXT: " + next.getTagName() + " " + next.getAttribute("class") + " " + next.getAttribute("id"));
			double nextWordCount = this.elementWordCount(next);
			System.out.println("NEXT WordCount: " + nextWordCount);
			//Not used in the current algorithm:
			//double nextLinkDensity = this.elementLinkDensity(next);;
			//TODO: add the link to the article for the numbers
			if(curLinkDensity <= 0.333333) {
				if(prevLinkDensity <= 0.555556) {
					if(curWordCount <= 16) {
						if(nextWordCount <= 15) {
							if(prevWordCount <= 4) {
//								type = ElementType.IrrelevantInformation;
							} else {
//								type = ElementType.RelevantText;
								this.textElements.add(element);
								System.out.println("ADDED: " + element.getTagName());
							}
						} else {
//							type = ElementType.RelevantText;
							this.textElements.add(element);
							System.out.println("ADDED: " + element.getTagName());
						}
					} else {
//						type = ElementType.RelevantText;
						this.textElements.add(element);
						System.out.println("ADDED: " + element.getTagName());
					}
				} else {
					if(curWordCount <= 40) {
						if(nextWordCount <= 17) {
//							type = ElementType.IrrelevantInformation;
						} else {
//							type = ElementType.RelevantText;
							this.textElements.add(element);
							System.out.println("ADDED: " + element.getTagName());
						}
					} else {
//						type = ElementType.RelevantText;
						this.textElements.add(element);
					}
				} 
			} else {
//				type = ElementType.IrrelevantInformation; 
			}
//			this.markedElements.put(element, type);
	}
	
	public double elementWordCount(WebElement element) throws UIMAException {
		String text = this.getElementText(element);
		List<String> words = TextTokenizer.tokenize(text);
		if(text.length()/10 > words.size()) {
			System.out.println("STRANGE:\n" + text);
		}
		return (double) words.size();
	}
	
	public double getSubElementCountByTagName(WebElement parent, String subelementTagName) {
		String script = "var childNodes = arguments[0][0].childNodes;"
				+ "		console.log('First: ' + arguments[0][0]);"
				+ "		console.log('Second: ' + arguments[0][1]);"
				+ "		var result = [];"
				+ "		for(var n = 0; n < childNodes.length; n++) {"
				+ "			console.log('ChildNode: ' + childNodes[n].nodeType);"
				+ "			if(childNodes[n].tagName === arguments[0][1]) {"
				+ "				result.push(childNodes[n]);"
				+ "			}"
				+ "		}"
				+ "		return result.length;";
		Long result = (Long)this.executeScript(script, new Object[] {parent, subelementTagName});
		System.out.println("Links: " + result);
		return result.doubleValue();
	}
	
	public double getSubElementCount(WebElement parent) {
		String script = "var childNodes = arguments[0].childNodes;"
						+ "return childNodes.length;";
		Long result = (Long)this.executeScript(script, parent);
		System.out.println("Total: " + result);
		return result.doubleValue();
	}
	
	public double elementLinkDensity(WebElement element) {
//		List<WebElement> links = element.findElements(By.tagName("a"));
		//TODO: check if it works with the cssSelector like this
//		List<WebElement> allSubElements = element.findElements(By.cssSelector("*"));
//		return (double)links.size() / allSubElements.size();
		double linksCount = this.getSubElementCountByTagName(element, "A");
		double totalSubElements = this.getSubElementCount(element);
		if(linksCount > 0) {
			return linksCount / totalSubElements;
		} 
		return 0;
	}
	
	private enum ElementType {
		RelevantText,
		HeaderText,
		IrrelevantInformation
	}
}
