package app.freerouting;

import app.freerouting.constants.Constants;
import app.freerouting.gui.DefaultExceptionHandler;
import app.freerouting.gui.WindowWelcome;
import app.freerouting.logger.FRLogger;
import app.freerouting.management.FRAnalytics;
import app.freerouting.management.VersionChecker;
import app.freerouting.settings.GlobalSettings;

import javax.swing.*;
import java.awt.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;

/* Entry point class of the application */
public class Freerouting
{
  public static final String WEB_URL = "https://www.freerouting.app";
  public static final String VERSION_NUMBER_STRING = "v" + Constants.FREEROUTING_VERSION + " (build-date: " + Constants.FREEROUTING_BUILD_DATE + ")";
  public static GlobalSettings globalSettings;

  /**
   * The entry point of the Freerouting application
   *
   * @param args
   */
  public static void main(String[] args)
  {
    // we have a special case if logging must be disabled before the general command line arguments
    // are parsed
    if (args.length > 0 && Arrays.asList(args).contains("-dl"))
    {
      // disable logging
      FRLogger.disableLogging();
    }
    else if (args.length > 0 && Arrays.asList(args).contains("-ll"))
    {
      // get the log level from the command line arguments
      int logLevelIndex = Arrays.asList(args).indexOf("-ll") + 1;
      if (logLevelIndex < args.length)
      {
        FRLogger.changeFileLogLevel(args[logLevelIndex]);
      }
    }

    FRLogger.traceEntry("MainApplication.main()");

    try
    {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (ClassNotFoundException | InstantiationException | UnsupportedLookAndFeelException | IllegalAccessException ex)
    {
      FRLogger.error(ex.getLocalizedMessage(), ex);
    }

    // Log system information
    FRLogger.info("Freerouting " + VERSION_NUMBER_STRING);
    Thread.setDefaultUncaughtExceptionHandler(new DefaultExceptionHandler());

    try
    {
      globalSettings = GlobalSettings.load();
      FRLogger.info("Settings were loaded from freerouting.json");
    } catch (Exception e)
    {
      // we don't want to stop if the configuration file doesn't exist
    }

    if ((globalSettings == null) || (globalSettings.version != Constants.FREEROUTING_VERSION))
    {
      globalSettings = new GlobalSettings();

      // save the default values
      try
      {
        GlobalSettings.save(globalSettings);
      } catch (Exception e)
      {
        // it's ok if we can't save the configuration file
      }
    }

    // get environment parameters and save them in the settings
    globalSettings.environmentSettings.freeroutingVersion = Constants.FREEROUTING_VERSION + "," + Constants.FREEROUTING_BUILD_DATE;
    globalSettings.environmentSettings.appStartedAt = Instant.now();
    globalSettings.environmentSettings.commandLineArguments = String.join(" ", args);
    globalSettings.environmentSettings.architecture = System.getProperty("os.name") + "," + System.getProperty("os.arch") + "," + System.getProperty("os.version");
    globalSettings.environmentSettings.java = System.getProperty("java.version") + "," + System.getProperty("java.vendor");
    globalSettings.environmentSettings.systemLanguage = Locale.getDefault().getLanguage() + "," + Locale.getDefault();
    globalSettings.environmentSettings.cpuCores = Runtime.getRuntime().availableProcessors();
    globalSettings.environmentSettings.ram = (int) (Runtime.getRuntime().maxMemory() / 1024 / 1024);
    FRLogger.debug(" Version: " + globalSettings.environmentSettings.freeroutingVersion);
    FRLogger.debug(" Command line arguments: '" + globalSettings.environmentSettings.commandLineArguments + "'");
    FRLogger.debug(" Architecture: " + globalSettings.environmentSettings.architecture);
    FRLogger.debug(" Java: " + globalSettings.environmentSettings.java);
    FRLogger.debug(" System Language: " + globalSettings.environmentSettings.systemLanguage);
    FRLogger.debug(" Hardware: " + globalSettings.environmentSettings.cpuCores + " CPU cores," + globalSettings.environmentSettings.ram + " MB RAM");
    FRLogger.debug(" UTC Time: " + globalSettings.environmentSettings.appStartedAt);

    // parse the command line arguments
    globalSettings.parseCommandLineArguments(args);

    FRLogger.debug(" GUI Language: " + globalSettings.current_locale);

    FRLogger.debug(" Host: " + globalSettings.host);

    // Get default screen device
    Toolkit toolkit = Toolkit.getDefaultToolkit();

    // Get screen resolution
    Dimension screenSize = toolkit.getScreenSize();
    int width = screenSize.width;
    int height = screenSize.height;

    // Get screen DPI
    int dpi = toolkit.getScreenResolution();
    FRLogger.debug(" Screen: " + width + "x" + height + ", " + dpi + " DPI");

    // initialize analytics
    FRAnalytics.setWriteKey(Constants.FREEROUTING_VERSION, "G24pcCv4BmnqwBa8LsdODYRE6k9IAlqR");
    int analyticsModulo = Math.max(globalSettings.usageAndDiagnosticData.analytics_modulo, 1);
    String userIdString = globalSettings.userProfileSettings.user_id.length() >= 4 ? globalSettings.userProfileSettings.user_id.substring(0, 4) : "0000";
    int userIdValue = Integer.parseInt(userIdString, 16);
    boolean allowAnalytics = !globalSettings.usageAndDiagnosticData.disable_analytics && (userIdValue % analyticsModulo == 0);
    if (!allowAnalytics)
    {
      FRLogger.debug("Analytics are disabled");
    }
    FRAnalytics.setEnabled(allowAnalytics);
    FRAnalytics.setUserId(globalSettings.userProfileSettings.user_id);
    FRAnalytics.identify();
    try
    {
      Thread.sleep(1000);
    } catch (Exception ignored)
    {
    }
    FRAnalytics.setAppLocation("app.freerouting.gui", "Freerouting");
    FRAnalytics.appStarted(Constants.FREEROUTING_VERSION, Constants.FREEROUTING_BUILD_DATE, String.join(" ", args), System.getProperty("os.name"), System.getProperty("os.arch"), System.getProperty("os.version"), System.getProperty("java.version"), System.getProperty("java.vendor"), Locale.getDefault(), globalSettings.current_locale, Runtime.getRuntime().availableProcessors(), (Runtime.getRuntime().maxMemory() / 1024 / 1024), globalSettings.host, width, height, dpi);

    // check for new version
    VersionChecker checker = new VersionChecker(Constants.FREEROUTING_VERSION);
    new Thread(checker).start();

    // Initialize the GUI
    if (globalSettings.guiSettings.isEnabled)
    {
      if (!WindowWelcome.InitializeGUI(globalSettings))
      {
        FRLogger.error("Couldn't initialize the GUI", null);
        return;
      }
    }

    if (globalSettings.apiServerSettings.isEnabled)
    {
      WindowWelcome.InitializeAPI(globalSettings.apiServerSettings);
    }

    try
    {
      GlobalSettings.save(globalSettings);
    } catch (Exception e)
    {
      // it's ok if we can't update the configuration file, it's just optional
    }

    FRLogger.traceExit("MainApplication.main()");
  }
}