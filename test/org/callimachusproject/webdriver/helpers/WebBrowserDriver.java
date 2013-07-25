package org.callimachusproject.webdriver.helpers;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.callimachusproject.engine.model.TermFactory;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchFrameException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

public class WebBrowserDriver {
	public RemoteWebDriver driver;

	public WebBrowserDriver(RemoteWebDriver driver) {
		this.driver = driver;
	}

	public RemoteWebDriver getRemoteWebDriver() {
		return driver;
	}

	public void quit() {
		driver.quit();
	}

	public void navigateTo(String ref) {
		String url = TermFactory.newInstance(driver.getCurrentUrl()).resolve(ref);
		driver.navigate().to(url);
	    waitForCursor();
	}

	public void focusInTopWindow() {
		driver.switchTo().window(driver.getWindowHandle());
	}

	public void focusInFrame(final String frameName) {
		new WebDriverWait(driver, 10).until(new ExpectedCondition<WebDriver>() {
			public WebDriver apply(WebDriver driver) {
				try {
					return driver.switchTo().frame(frameName);
				} catch (NoSuchFrameException e) {
					return null;
				}
			}
		});
	
	}

	public void focusInFrame(int... frames) {
		driver.switchTo().window(driver.getWindowHandle());
		for (final int frame : frames) {
			new WebDriverWait(driver, 10)
					.until(new ExpectedCondition<WebDriver>() {
						public WebDriver apply(WebDriver driver) {
							try {
								return driver.switchTo().frame(frame);
							} catch (NoSuchFrameException e) {
								return null;
							}
						}
					});
		}
	}

	public void click(By locator) {
		driver.findElement(locator).click();
	}

	public void clickInFrame(By locator, int... frames) {
		focusInFrame(frames);
		WebElement element = driver.findElement(locator);
	    this.driver.executeScript("arguments[0].click();", element);
	}

	public void type(By locator, String text) {
		WebElement element = driver.findElement(locator);
		element.clear();
		sendKeys(element, text);
	}

	public void confirm(String msg) {
		Alert alert = driver.switchTo().alert();
		assertTrue(alert.getText().contains(msg));
	    alert.accept();
	    waitForCursor();
	}

	public WebElement getActiveFrameElement(int... frames) {
		focusInFrame(frames);
		return driver.switchTo().activeElement();
	}

	public void mouseOver(By locator) {
	    new Actions(driver).moveToElement(driver.findElement(locator)).build().perform();
	}

	public void sendKeys(By locator, CharSequence... keys) {
		sendKeys(driver.findElement(locator), keys);
	}

	public void sendKeys(WebElement element, CharSequence... keys) {
		List<CharSequence> list = new ArrayList<CharSequence>(keys.length);
		for (CharSequence key : keys) {
			if (key instanceof String && ((String) key).contains("-")) {
				for (String text : ((String) key).split("-")) {
					list.add(text);
					list.add(Keys.SUBTRACT);
				}
				list.remove(list.size() - 1);
			} else {
				list.add(key);
			}
		}
		element.sendKeys(list.toArray(new CharSequence[list.size()]));
	}

	public void waitUntilTextPresent(final String needle) {
	    waitForCursor();
		Wait<WebDriver> wait = new WebDriverWait(driver, 30);
		Boolean present = wait.until(new ExpectedCondition<Boolean>() {
			public Boolean apply(WebDriver driver) {
				if (driver.findElement(By.cssSelector("BODY")).getText()
						.contains(needle)) {
					return true;
				} else {
					return null;
				}
			}
		});
		assertTrue(present);
	}

	public void waitForCursor() {
		Wait<WebDriver> wait = new WebDriverWait(driver, 30);
		Boolean present = wait.until(new ExpectedCondition<Boolean>() {
			public Boolean apply(WebDriver wd) {
				String className = (String) driver.executeScript("if (document.body) return window.document.documentElement.className; else return 'wait';");
				if (!className.contains("wait")) {
					return true;
				} else {
					return null;
				}
			}
		});
		assertTrue(present);
	}

}
