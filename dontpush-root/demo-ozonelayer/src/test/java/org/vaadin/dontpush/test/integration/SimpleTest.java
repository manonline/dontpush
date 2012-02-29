package org.vaadin.dontpush.test.integration;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

public class SimpleTest {
	
	
	@Test
	public void testWithFF() {
		prepareFFDriver();
		doTest();
	}
	
	@Test
	public void testWithChrome() {
		prepareChromeDriver();
		doTest();
	}
	
	WebDriver driver;
	
	public void prepareFFDriver() {
		driver = new FirefoxDriver();
		driver.manage().timeouts().implicitlyWait(3, TimeUnit.SECONDS);
	}
	
	public void prepareChromeDriver() {
        if (System.getProperty("webdriver.chrome.driver") == null) {
            System.setProperty("webdriver.chrome.driver",
                    "/usr/local/bin/chromedriver");
        }
        driver = new ChromeDriver();
	}
	
	public void doTest() {
		driver.get("http://localhost:8080/");
		// don't know why exactly I needed with ChromeDriver
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		WebElement tf = driver.findElement(By.className("v-textfield"));
		
		tf.sendKeys("Moi!");
		
		 List<WebElement> buttons = driver.findElements(By.className("v-button"));
		 WebElement button1 = buttons.get(0);
		 WebElement button2 = buttons.get(1);
		 button2.click();
		 button1.click();
		 
		 WebElement not = driver.findElement(By.className("v-Notification"));
		 assertEquals("clicked", not.getText());

		
		// wait until the thread has stopped
		try {
			Thread.sleep(23000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		List<WebElement> labels = driver.findElements(By.className("v-label"));
		WebElement msglabel = labels.get(labels.size() -2);
		WebElement threadstoppedlable = labels.get(labels.size() -1);
		
		assertEquals("Test thread done", threadstoppedlable.getText());
		
		assertTrue(msglabel.getText().contains("Moi!"));
		
		assertEquals("Test thread done", threadstoppedlable.getText());

		
	}
	
	
	@After
	public void closeDriver() {
		driver.close();
	}

}
