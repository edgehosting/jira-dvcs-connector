package it.com.atlassian.jira.plugins.dvcs;

import com.atlassian.webdriver.AtlassianWebDriver;
import com.atlassian.webdriver.testing.rule.WebDriverSupport;
import com.google.common.base.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

import java.io.File;
import javax.annotation.Nonnull;
import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * A TestNG listener for taking screen-shots when a WebDriver test fails. It will also dump the html source of the page to
 * the target/webDriverTests directory.
 *
 * @see com.atlassian.webdriver.testing.rule.WebDriverScreenshotRule
 */
public class WebDriverScreenshotListener extends TestListenerAdapter
{
    private static final Logger log = LoggerFactory.getLogger(WebDriverScreenshotListener.class);

    private final WebDriverSupport<AtlassianWebDriver> webDriverSupport;
    private final File artifactDir;

    private static File defaultArtifactDir()
    {
        return new File("target/webdriverTests");
    }

    protected WebDriverScreenshotListener(@Nonnull WebDriverSupport<AtlassianWebDriver> support, @Nonnull File artifactDir)
    {
        this.webDriverSupport = checkNotNull(support, "support");
        this.artifactDir = artifactDir;
    }

    public WebDriverScreenshotListener(@Nonnull Supplier<? extends AtlassianWebDriver> driverSupplier, @Nonnull File artifactDir)
    {
        this(WebDriverSupport.forSupplier(driverSupplier), artifactDir);
    }

    public WebDriverScreenshotListener(@Nonnull Supplier<? extends AtlassianWebDriver> driverSupplier)
    {
        this(driverSupplier, defaultArtifactDir());
    }

    @Inject
    public WebDriverScreenshotListener(@Nonnull AtlassianWebDriver webDriver)
    {
        this(WebDriverSupport.forInstance(webDriver), defaultArtifactDir());
    }

    public WebDriverScreenshotListener()
    {
        this(WebDriverSupport.fromAutoInstall(), defaultArtifactDir());
    }

    @Override
    public void onTestFailure(final ITestResult tr)
    {
        super.onTestFailure(tr);

        captureScreenshotAndSource(tr);
    }

    @Override
    public void onConfigurationFailure(final ITestResult tr)
    {
        super.onConfigurationFailure(tr);

        captureScreenshotAndSource(tr);
    }

    private void captureScreenshotAndSource(final ITestResult tr)
    {
        createTargetDirIfNotExists(tr);

        // capture screenshot and html source
        final AtlassianWebDriver driver = webDriverSupport.getDriver();
        final File dumpFile = getTargetFile(tr, "html");
        final File screenShotFile = getTargetFile(tr, "png");
        log.info("----- {}.{} failed. ", tr.getTestClass().getName(), tr.getMethod().getMethodName());
        log.info("----- At page: " + driver.getCurrentUrl());
        log.info("----- Dumping page source to {} and screenshot to {}", dumpFile.getAbsolutePath(),
                screenShotFile.getAbsolutePath());
        driver.dumpSourceTo(dumpFile);
        driver.takeScreenshotTo(screenShotFile);
    }

    private void createTargetDirIfNotExists(final ITestResult tr)
    {
        // create the dir if not exists
        File dir = getTargetDir(tr);
        if (!dir.exists())
        {
            checkState(dir.mkdirs(), "Unable to create screenshot output directory " + dir.getAbsolutePath());
        }
    }

    private File getTargetDir(ITestResult tr)
    {
        return new File(artifactDir, tr.getTestClass().getName());
    }

    private File getTargetFile(ITestResult tr, String extension)
    {
        return new File(getTargetDir(tr), tr.getMethod().getMethodName() + "." + extension);
    }
}
