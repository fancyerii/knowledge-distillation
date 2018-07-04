package com.github.fancyerii.test.webdriver;
 
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions; 

public class TestChromeDriver {

	public static void main(String[] args) throws Exception {
		System.setProperty("webdriver.chrome.driver","/home/mc/soft/chromedriver");
		ChromeOptions options = new ChromeOptions();
        //options.addArguments("headless");
        //options.addArguments("window-size=1200x600");
		WebDriver driver = new ChromeDriver(options);
		driver.get("https://mail.163.com/");
		WebElement elem=driver.findElement(By.xpath("//DIV[@id='lbNormal']"));
		elem.click();
		Thread.sleep(1000);
		driver.switchTo().frame("x-URS-iframe");
		elem=driver.findElement(By.xpath("//INPUT[@name='email']"));
		elem.sendKeys("crawl_tutorial");
		elem=driver.findElement(By.xpath("//INPUT[@name='password']"));
		elem.sendKeys("crawl_123");
		elem=driver.findElement(By.xpath("//A[@id='dologin']"));
		elem.click();
		Thread.sleep(3000);
		String html=driver.getPageSource();
		System.out.println(html);
		elem=driver.findElement(By.xpath("//DIV[@class='tY0']//A[text()='退出']"));
		elem.click();
		Thread.sleep(10000);
		driver.quit();
	}

}
