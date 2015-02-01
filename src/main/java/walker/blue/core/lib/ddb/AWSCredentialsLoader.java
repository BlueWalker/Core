package walker.blue.core.lib.ddb;

import android.util.Log;

import com.amazonaws.auth.AWSCredentials;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class in charge of loading AWS keys from the device
 *
 * TODO: Keys are currently being stored as plaintext. Possible encrypt them.
 */
public class AWSCredentialsLoader {

    /**
     * Path to the file containing the credentials
     */
    private static final String CREDENTIALS_LOCATION = "/sdcard/BlueWalker/awscredentials";
    /**
     * Keyword for the Access Key
     */
    private static final String ACCESS_KEY = "access_key";
    /**
     * Keyword for the Secret Key
     */
    private static final String SECRET_KEY = "secret_key";
    /**
     * Log Message for when credentials are missing from the credentials file
     */
    private static final String LOG_MISSING_CREDETIALS = "Cant create AWSCredentials. Credentials missing.";
    /**
     * Log message when loading the credentials fails
     */
    private static final String LOG_LOADING_FAILED = "Failed loading AWSCredentials.";


    /**
     * Private constructor so class cant be initialized
     */
    private AWSCredentialsLoader() {}

    /**
     * Loads the credentials for AWS from the device
     * Cant use try with resources in order to support Api 18 :(
     *
     * @return AWSCredentials
     */
    public static AWSCredentials loadCredentials() {
        FileReader credentialsReader = null;
        BufferedReader credentialsFile = null;
        try {
            credentialsReader = new FileReader(CREDENTIALS_LOCATION);
            credentialsFile = new BufferedReader(credentialsReader);
            final Map<String, String> credentials = new HashMap<>();
            String buffer;
            while((buffer = credentialsFile.readLine()) != null) {
                final String[] split = buffer.split("=");
                if (split.length > 1) {
                    credentials.put(split[0], split[1]);
                }
            }
            return buildCredentials(credentials);
        } catch (final Exception e) {
            Log.d(AWSCredentials.class.getName(), LOG_LOADING_FAILED);
        } finally {
            try {
                if (credentialsReader != null) {
                    credentialsReader.close();
                }
                if (credentialsFile != null) {
                    credentialsFile.close();
                }
            } catch (final IOException e) {/*Do Nothing*/}
        }
        return null;
    }

    /**
     * Builds the AWSCredentials using the given map of credentials
     *
     * @param credentials Map of Strings to Strings
     * @return AWSCredentials
     */
    private static AWSCredentials buildCredentials(final Map<String, String> credentials) {
        if (credentials.containsKey(ACCESS_KEY) &&  credentials.containsKey(SECRET_KEY)) {
            return new AWSCredentials() {
                @Override
                public String getAWSAccessKeyId() {
                    return credentials.get(ACCESS_KEY);
                }

                @Override
                public String getAWSSecretKey() {
                    return credentials.get(SECRET_KEY);
                }
            };
        } else {
            Log.d(getClassName(), LOG_MISSING_CREDETIALS);
            return null;
        }
    }

    /**
     * Helper class to get the class name of a static class
     *
     * @return String
     */
    private static String getClassName() {
        return AWSCredentialsLoader.getClassName();
    }
}
