package io.supertokens.cronjobs.deleteExpiredTotpTokens;

import io.supertokens.Main;
import io.supertokens.ResourceDistributor;
import io.supertokens.config.Config;
import io.supertokens.pluginInterface.STORAGE_TYPE;
import io.supertokens.pluginInterface.totp.sqlStorage.TOTPSQLStorage;
import io.supertokens.cronjobs.CronTask;
import io.supertokens.cronjobs.CronTaskTest;
import io.supertokens.storageLayer.StorageLayer;
import io.supertokens.output.Logging;

public class DeleteExpiredTotpTokens extends CronTask {

    public static final String RESOURCE_KEY = "io.supertokens.cronjobs.deleteExpiredTotpTokens.DeleteExpiredTotpTokens";

    private DeleteExpiredTotpTokens(Main main) {
        super("DeleteExpiredTotpTokens", main);
    }

    public static DeleteExpiredTotpTokens getInstance(Main main) {
        ResourceDistributor.SingletonResource instance = main.getResourceDistributor().getResource(RESOURCE_KEY);
        if (instance == null) {
            instance = main.getResourceDistributor().setResource(RESOURCE_KEY, new DeleteExpiredTotpTokens(main));
        }
        return (DeleteExpiredTotpTokens) instance;
    }

    @Override
    protected void doTask() throws Exception {
        if (StorageLayer.getStorage(this.main).getType() != STORAGE_TYPE.SQL) {
            return;
        }

        TOTPSQLStorage storage = StorageLayer.getTOTPStorage(this.main);

        long rateLimitResetInMs = Config.getConfig(this.main).getTotpRateLimitCooldownTimeSec() * 1000;
        long expiredBefore = System.currentTimeMillis() - rateLimitResetInMs;

        // We will only remove expired codes that have been expired for longer
        // than rate limiting duration. This ensures that this DB query
        // doesn't delete totp codes that keep the rate limiting active for
        // the expected cooldown duration.
        int deletedCount = storage.removeExpiredCodes(expiredBefore);
        Logging.debug(this.main, "Cron DeleteExpiredTotpTokens deleted " + deletedCount + " expired TOTP codes");
    }

    @Override
    public int getIntervalTimeSeconds() {
        if (Main.isTesting) {
            Integer interval = CronTaskTest.getInstance(main).getIntervalInSeconds(RESOURCE_KEY);
            if (interval != null) {
                return interval;
            }
        }

        return 3600; // every hour
    }

    @Override
    public int getInitialWaitTimeSeconds() {
        if (!Main.isTesting) {
            return getIntervalTimeSeconds();
        } else {
            return 0;
        }
    }

}
