package com.kalessil.phpStorm.phpInspectionsEA;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.extensions.PluginId;
import com.kalessil.phpStorm.phpInspectionsEA.license.*;
import com.kalessil.phpStorm.phpInspectionsEA.utils.analytics.AnalyticsUtil;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EAApplicationComponent implements ApplicationComponent {
    private boolean updated;
    private boolean updateNotificationShown;

    static private IdeaPluginDescriptor plugin;
    static private LicenseService licenseService;

    public static boolean areFeaturesEnabled() {
        return licenseService != null && licenseService.shouldAllowUsage();
    }

    @Nullable
    public static LicenseService getLicenseService() {
        return licenseService;
    }

    @Nullable
    public static IdeaPluginDescriptor getPluginDescriptor() {
        return plugin;
    }
    
    @NotNull
    public static EAApplicationComponent getInstance() {
        return ApplicationManager.getApplication().getComponent(EAApplicationComponent.class);
    }

    private void initLicensing() {
        licenseService = licenseService == null ? new LicenseService() : licenseService;
        if (licenseService.shouldCheckPluginLicense()) {
            try {
                licenseService.initializeClient();
                if (!licenseService.isActiveLicense() && !licenseService.isActiveTrialLicense()) {
                    final String message;
                    if (licenseService.isActivatedLicense()) {
                        message = "The license has expired. Please <a href='#activate'>provide</a> a new one (you can purchase it <a href='#buy'>here</a>).";
                    } else {
                        message = "Please <a href='#activate'>provide</a> a license key (you can purchase one <a href='#buy'>here</a> or <a href='#try'>start</a> a free trial).";
                    }
                    throw new RuntimeException(message);
                }
            } catch (Throwable failure) {
                final LicenseService service  = licenseService;
                final String message          = failure.getMessage();
                final String pluginName       = plugin.getName();
                final NotificationGroup group = new NotificationGroup(pluginName, NotificationDisplayType.STICKY_BALLOON, true);
                Notifications.Bus.notify(group.createNotification(
                    "<b>" + pluginName + "</b>",
                    message == null ? failure.getClass().getName() : message,
                    NotificationType.WARNING,
                    EaNotificationLinksHandler.TAKE_LICENSE_ACTION_LISTENER.withActionCallback(action -> {
                        switch (action) {
                            case "#try":      (new StartTrialAction()).perform(service, plugin);      break;
                            case "#buy":      (new PurchaseLicenseAction()).perform(plugin);          break;
                            case "#activate": (new ActivateLicenseAction()).perform(service, plugin); break;
                        }
                    })
                ));
            }
        }
    }

    @Override
    public void initComponent() {
        plugin = plugin == null ? PluginManager.getPlugin(PluginId.getId("com.kalessil.phpStorm.phpInspectionsEA")) : plugin;
        if (plugin == null) {
            return;
        }

        final EASettings settings = EASettings.getInstance();

        /* collect installation events (anonymous) */
        this.updated = !plugin.getVersion().equals(settings.getVersion());
        if (this.updated) {
            settings.setVersion(plugin.getVersion());
            AnalyticsUtil.registerPluginEvent(settings, "install", settings.getOldestVersion());
        }
        AnalyticsUtil.registerPluginEvent(settings, "run", settings.getOldestVersion());

        /* collect exceptions */
        final FileAppender appender = new FileAppender() {
            @Override
            public void append(@NotNull LoggingEvent event) {
                final ThrowableInformation exceptionDetails = event.getThrowableInformation();
                if (exceptionDetails != null) {
                    AnalyticsUtil.registerLoggedException(
                        settings.getVersion(),
                        settings.getUuid(),
                        exceptionDetails.getThrowable()
                    );
                }
            }
        };
        appender.setName("ea-exceptions-tracker");
        Logger.getRootLogger().addAppender(appender);

        this.initLicensing();
    }

    @Override
    public void disposeComponent() {
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "EAApplicationComponent";
    }

    boolean isUpdated() {
        return this.updated;
    }

    boolean isUpdateNotificationShown() {
        return this.updateNotificationShown;
    }

    void setUpdateNotificationShown() {
        this.updateNotificationShown = true;
    }
}
