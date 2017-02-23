package com.jeffthefate.dmbquiz;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.ardverk.collection.PatriciaTrie;
import org.ardverk.collection.StringKeyAnalyzer;
import org.ardverk.collection.Trie;
import org.json.JSONArray;
import org.json.JSONException;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.logging.Logger;
import com.backendless.persistence.BackendlessDataQuery;
import com.backendless.persistence.QueryOptions;
import com.jeffthefate.stacktrace.ExceptionHandler;
import com.jeffthefate.stacktrace.ExceptionHandler.OnStacktraceListener;

/**
 * Used as a holder of many values and objects for the entire application.
 *
 * For Backendless error codes, see:
 * https://backendless.com/documentation/data/android/error_handling.htm
 *
 * @author Jeff Fate
 */
@SuppressLint("ShowToast")
public class ApplicationEx extends Application implements OnStacktraceListener {

    private Logger logger = Backendless.Logging.getLogger(ApplicationEx.class);

    private static boolean mHasConnection = false;
    private static boolean mIsActive = false;
    /**
     * Path to the application's external cache
     */
    public static String cacheLocation = null;
    private static Toast mToast;
    /**
     * Last setlist reported, broken in to an
     * {@link java.util.ArrayList ArrayList} of Strings
     */
    public static ArrayList<String> setlistList;
    /**
     * Location of notification sound for the last reported song
     */
    public static Uri notificationSound;
    private static Trie<String, SongInfo> songMap;
    /**
     * Time date format for the updated time stamp
     */
    public static SimpleDateFormat df = new SimpleDateFormat("h:mm a zzz", Locale.getDefault());
    
    private static Bitmap portraitBackgroundBitmap;
    private static Bitmap landBackgroundBitmap;
    private static Bitmap portraitSetlistBitmap;
    private static Bitmap landSetlistBitmap;
    
    private static boolean isDownloading = false;
    
    private static float textViewHeight = 0.0f;
    
    private static List<String> serialsList;
    
    /**
     * Holds an image and audio clip that are associated with each other
     * @author Jeff Fate
     */
    private static class SongInfo {
        private int image;
        private int audio;
        
        private SongInfo(int image, int audio) {
            this.image = image;
            this.audio = audio;
        }
        
        public int getImage() {
            return image;
        }
        
        public int getAudio() {
            return audio;
        }
    }
    
    /**
     * Singleton of the SharedPreferences used by the application
     * @author Jeff
     */
    public static class SharedPreferencesSingleton {
    	private SharedPreferencesSingleton() {}
    	
    	private static SharedPreferences sharedPrefs = null;
        private static Resources resources = null;
    	
    	/**
    	 * Get an instance, creating it if necessary, of the shared preferences
    	 * object for the application
    	 * @return shared preferences object for the application's preferences
    	 */
    	public static SharedPreferences instance(Context context) {
    		if (sharedPrefs == null) {
                sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            }
            if (resources == null) {
                resources = ResourcesSingleton.instance(context);
            }
    		return sharedPrefs;
    	}
    	
    	public static void toggleBoolean(int resId, boolean def) {
            sharedPrefs.edit().putBoolean(resources.getString(resId),
                    !sharedPrefs.getBoolean(resources.getString(resId),def)).apply();
    	}
    	
    	public static void putBoolean(int resId, boolean newValue) {
            sharedPrefs.edit().putBoolean(resources.getString(resId), newValue).apply();
    	}
    	
    	public static void putString(int resId, String newValue) {
            sharedPrefs.edit().putString(resources.getString(resId), newValue).apply();
    	}
    	
    	public static void putInt(int resId, int newValue) {
            sharedPrefs.edit().putInt(resources.getString(resId), newValue).apply();
    	}
    	
    }
    
    /**
     * Singleton of the DatabaseHelper used by the application
     * @author Jeff
     */
    public static class DatabaseHelperSingleton {
    	private DatabaseHelperSingleton() {}
    	
    	private static DatabaseHelper dbHelper = null;
    	
    	/**
    	 * Get an instance, creating it if necessary, of the shared preferences
    	 * object for the application
    	 * @return shared preferences object for the application's preferences
         * @param context
    	 */
    	public static DatabaseHelper instance(Context context) {
    		if (dbHelper == null) {
                dbHelper = DatabaseHelper.getInstance(context);
            }
    		return dbHelper;
    	}
    }
    
    /**
     * Singleton of the Resources used by the application
     * @author Jeff
     */
    public static class ResourcesSingleton {
    	private ResourcesSingleton() {}
    	
    	private static Resources res = null;
    	
    	/**
    	 * Get an instance, creating it if necessary, of the shared preferences
    	 * object for the application
    	 * @return shared preferences object for the application's preferences
    	 */
    	public static Resources instance(Context context) {
    		if (res == null) {
                res = context.getResources();
            }
    		return res;
    	}
    }
    
    public static class FileCacheSingleton {
    	private static FileCacheSingleton fileCacheSingleton;
    	private static String cacheLocation;
    	private static String fileLocation;
    	
    	public static FileCacheSingleton instance(Context context) {
    		if (fileCacheSingleton == null) {
    			fileCacheSingleton = new FileCacheSingleton();
    		}
    		if (cacheLocation == null && context.getExternalCacheDir() != null &&
                    context.getFilesDir() != null) {
    			cacheLocation = context.getExternalCacheDir().getAbsolutePath();
    			fileLocation = context.getFilesDir().getAbsolutePath();
    		}
    		return fileCacheSingleton;
    	}
    	
    	public String createLocation(String dir) {
    		File path = new File(dir);
            path.setExecutable(true, false);
            path.setReadable(true, false);
            path.setWritable(true, false);
            path.mkdirs();
            return path.getAbsolutePath();
    	}
    	
    	public boolean saveSerializableToFile(String file,
    			Serializable object) {
    		return saveSerializable(file, fileLocation, object);
    	}
    	
    	public Serializable readSerializableFromFile(String file) {
    		return readSerializable(file, fileLocation);
    	}
    	
    	public boolean saveSerializable(String file, String location,
    			Serializable object) {
    		File cacheFile = new File(location + File.separatorChar + file);
    		try {
    			FileOutputStream fileOutputStream =
    					new FileOutputStream(cacheFile);
    			ObjectOutputStream objectOutputStream = new ObjectOutputStream(
                        fileOutputStream);
                objectOutputStream.writeObject(object);
                objectOutputStream.close();
    			fileOutputStream.close();
    		} catch (FileNotFoundException e) {
    			e.printStackTrace();
    			return false;
    		} catch (IOException e) {
    			e.printStackTrace();
    			return false;
    		}
    		return true;
    	}
    	
    	public Serializable readSerializable(String file,
    			String location) {
    		Serializable object;
    		try {
                FileInputStream fileInputStream = new FileInputStream(
                		location + File.separatorChar + file);
                ObjectInputStream objectInputStream = new ObjectInputStream(
                        fileInputStream);
                object = (Serializable) objectInputStream.readObject();
                objectInputStream.close();
                fileInputStream.close();
            } catch (FileNotFoundException e) {
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
    		return object;
    	}
    }
    
    @SuppressLint("NewApi")
	@Override
    public void onCreate() {
        super.onCreate();
        // make sure AsyncTask is loaded in the Main thread
        // https://code.google.com/p/android/issues/detail?id=20915
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                return null;
            }
        };
        mToast = Toast.makeText(this, "", Toast.LENGTH_LONG);
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            cacheLocation = getExternalCacheDir().getAbsolutePath();
            File path = new File(cacheLocation + Constants.SCREENS_LOCATION);
            path.setExecutable(true, false);
            path.setReadable(true, false);
            path.setWritable(true, false);
            path.mkdirs();
            path = new File(cacheLocation + Constants.AUDIO_LOCATION);
            path.setExecutable(true, false);
            path.setReadable(true, false);
            path.setWritable(true, false);
            path.mkdirs();
        }
        /*
        Parse.initialize(this, "6pJz1oVHAwZ7tfOuvHfQCRz6AVKZzg1itFVfzx2q",
                "2ocGkdBygVyNStd8gFQQgrDyxxZJCXt3K1GbRpMD");
        */
        Backendless.initApp(this, "F1672081-F7D4-EF63-FFB1-BB39109F8500",
                "FDD4D19E-F228-22AC-FF44-2F9FA5528300", "v1");
        /**
        Parse.initialize(this, "ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R",
                "hpTbnpuJ34zAFLnpOAXjH583rZGiYQVBWWvuXsTo");
        ParseFacebookUtils.initialize(this);
        ParseTwitterUtils.initialize("xWnkCrbGRNGMVs2HDyShQ",
                "xaDerd1mUtfmjyuANARkuvNBrQFgsVpQmhYWDjnirOw");
         */
        ExceptionHandler.register(this);
        ConnectivityManager connMan = ((ConnectivityManager) getSystemService(
                Context.CONNECTIVITY_SERVICE));
        NetworkInfo nInfo = connMan.getActiveNetworkInfo();
        if (nInfo != null) {
            mHasConnection = nInfo.isConnected();
        }
        DatabaseHelperSingleton.instance(getApplicationContext()).checkUpgrade();
        //setlist = "Jun 1 2013\nDave Matthews Band\nBlossom Music Center\nCuyahoga Falls, OH\n\nDancing Nancies ->\nWarehouse\nThe Idea Of You\nBelly Belly Nice\nSave Me\nCaptain\nSeven";
        //setlistStamp = "Updated:\n8:16 PDT";
        generateSongMap(getApplicationContext());
        Backendless.Messaging.registerDevice("596253330527", "setlist", new AsyncCallback<Void>() {
            @Override
            public void handleResponse(Void response) {
                Log.i(Constants.LOG_TAG, "Registered for notifications");
            }

            @Override
            public void handleFault(BackendlessFault fault) {
                // TODO Handle this the same way as other errors connecting to the backend
            }
        });
        String notificationType = ResourcesSingleton.instance(getApplicationContext()).getString(
        		R.string.notificationtype_key);
        try {
            SharedPreferencesSingleton.putInt(R.string.notificationtype_key,
                    SharedPreferencesSingleton.instance(getApplicationContext()).getBoolean(
                            notificationType, false) ? 1 : 0);
        } catch (ClassCastException e) {
            // TODO
        }
        getSetlist(getApplicationContext());
        /*
        if (SharedPreferencesSingleton.instance().getInt(notificationType, 0) ==
        		2 && !isDownloading())
	        downloadSongClips(DatabaseHelperSingleton.instance()
	        		.getNotificatationsToDownload());
	    */
        // Log.v(Constants.LOG_TAG, "SERIAL: " + Build.SERIAL);

        serialsList = new ArrayList<>(0);
        // Jay's old Incredible
        serialsList.add("HT1BNS215989");
        // Jay's old Rezound?
        serialsList.add("0146914813011017");
        // Jeff's Nexus 5
        serialsList.add("031c77d20935c90d");
        // Jay's LG G2
        serialsList.add("017d103f6390e474");
    }

    @Override
    public void onStacktrace(String appPackage, String packageVersion,
            String deviceModel, String androidVersion, String stacktrace) {
        com.jeffthefate.dmbquiz.Log log = new com.jeffthefate.dmbquiz.Log();
        log.setAndroidVersion(androidVersion);
        log.setPackageVersion(packageVersion);
        log.setDeviceModel(deviceModel);
        log.setAppPackage(appPackage);
        log.setStacktrace(stacktrace);
        Backendless.Persistence.of(com.jeffthefate.dmbquiz.Log.class).save(log,
                new AsyncCallback<com.jeffthefate.dmbquiz.Log>() {
            @Override
            public void handleResponse(com.jeffthefate.dmbquiz.Log response) {
                // TODO
            }

            @Override
            public void handleFault(BackendlessFault fault) {
                // TODO Handle like other actions that fail talking to backend
            }
        });
    }

    /**
     * Reports question to Parse to indicate there is an error in the question
     * or answer
     * @param questionId	identifier in the Parse class
     * @param questionText	question text
     * @param answer		answer text
     * @param score			current score
     */
    public static void reportQuestion(String questionId, String questionText,
            String answer, String score) {
        if (questionId != null && questionText != null && answer != null && score != null) {
            Question question = new Question();
            question.setObjectId(questionId);
            question.setQuestion(questionText);
            question.setAnswer(answer);
            try {
                question.setScore(Integer.parseInt(score));
            } catch (NumberFormatException e) {
                // TODO
            }

            Report report = new Report();
            report.setQuestion(question);

            Backendless.Persistence.save(report, new AsyncCallback<Report>() {
                @Override
                public void handleResponse(Report response) {
                    showLongToast("Report sent, thank you");
                }

                @Override
                public void handleFault(BackendlessFault fault) {
                    // TODO
                }
            });
        } else {
            // TODO
        }
    }

    /**
     * Set indication if there is a network connection
     * @param hasConnection true if application is reporting a connection
     */
    public static void setConnection(boolean hasConnection) {
        mHasConnection = hasConnection;
    }

    /**
     * Reports if the application has a network connection
     * @return true if the application reports having a connection
     */
    public static boolean hasConnection() {
        return mHasConnection;
    }

    /**
     * Remove all cached files for this application, including directories
     */
    public void clearApplicationData() {
        File cache = getCacheDir();
        File appDir = new File(cache.getParent());
        if (appDir.exists()) {
            String[] children = appDir.list();
            for (String s : children) {
                if (!s.equals("lib"))
                    deleteDir(new File(appDir, s));
            }
        }
    }

    /**
     * Helper to delete a directory in file structure
     * @param dir directory to be deleted
     * @return true if delete was successful
     */
    public static void deleteDir(File dir) {
        if (dir != null) {
            if (dir.isDirectory()) {
                String[] children = dir.list();
                for (String child : children) {
                    deleteDir(new File(dir, child));
                }
            } else {
                dir.delete();
            }
        }
    }

    /**
     * Determines if the app is currently active, visible to the user
     * @return true if active, false otherwise
     */
    public static boolean isActive() {
        return mIsActive;
    }

    /**
     * Indicate that the application is active, visible to user
     */
    public static void setActive() {
        mIsActive = true;
    }

    /**
     * Indicate that the application is not active, visible to user
     */
    public static void setInactive() {
        mIsActive = false;
    }

    /**
     * Set preference of {@link java.util.ArrayList ArrayList} of
     * {@link java.lang.String Strings} to key
     * @param key		corresponding to this preference
     * @param answers	{@link java.util.ArrayList ArrayList} of
     * 					{@link java.lang.String Strings} to assign to the key
     */
    public static void setStringArrayPref(Context context, String key, ArrayList<String> answers) {
        SharedPreferences.Editor editor = SharedPreferencesSingleton.instance(context).edit();
        if (answers == null) {
            editor.remove(key);
        }
        else {
            JSONArray array = new JSONArray(answers);
            if (!answers.isEmpty()) {
                editor.putString(key, array.toString());
            }
            else {
                editor.putString(key, null);
            }
        }
        editor.apply();
    }

    /**
     * Get string array preference given a specific key
     * @param key used to find preference
     * @return an {@link java.util.ArrayList ArrayList} of
     * 		   {@link java.lang.String Strings} containing the preference
     * 		   matching the given key
     */
    public static ArrayList<String> getStringArrayPref(Context context, String key) {
        String json = SharedPreferencesSingleton.instance(context).getString(key, null);
        ArrayList<String> answers = null;
        if (json != null) {
            answers = new ArrayList<>();
            try {
                JSONArray array = new JSONArray(json);
                for (int i = 0; i < array.length(); i++) {
                    answers.add(array.optString(i));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return answers;
    }

    /**
     * Display an application-wide toast message, with short timeout
     * @param message string to display
     */
    public static void showShortToast(String message) {
    	if (mToast != null) {
    		mToast.setText(message);
    		mToast.setDuration(Toast.LENGTH_SHORT);
    		mToast.show();
    	}
    }

    /**
     * Display an application-wide toast message, with long timeout
     * @param message string to display
     */
    public static void showLongToast(String message) {
    	if (mToast != null) {
    		mToast.setText(message);
    		mToast.setDuration(Toast.LENGTH_LONG);
    		mToast.show();
    	}
    }

    /**
     * Display an application-wide toast message, with long timeout
     * @param messageId resource id of string to display
     */
    public static void showLongToast(Context context, int messageId) {
    	showLongToast(context.getString(messageId));
    }

    /**
     * Fetch the most recent setlist from the Parse service, along with the last
     * updated time and send broadcast to any receivers that there is a new
     * setlist to show.
     */
    public static void getSetlist(final Context context) {
        QueryOptions queryOptions = new QueryOptions();
        List<String> sortBy = new ArrayList<>();
        sortBy.add("setDate DESC");
        queryOptions.setSortBy(sortBy);
        queryOptions.setPageSize(1);
        queryOptions.setOffset(0);
        BackendlessDataQuery query = new BackendlessDataQuery();
        query.setQueryOptions(queryOptions);
        Backendless.Persistence.of(Setlist.class).find(query,
                new AsyncCallback<BackendlessCollection<Setlist>>() {

            Intent intent = new Intent(Constants.ACTION_UPDATE_SETLIST);
            String setlist;
            String setStamp;

            private void setlistWork(String setlist, String setStamp) {
                Editor editor = SharedPreferencesSingleton.instance(context).edit();
                editor.putString(ResourcesSingleton.instance(context).getString(
                        R.string.setlist_key), setlist);
                editor.putString(ResourcesSingleton.instance(context).getString(
                        R.string.setstamp_key), setStamp);
                Log.w(Constants.LOG_TAG, "updating setlist_key");
                editor.apply();
                parseSetlist(setlist);
            }

            @Override
            public void handleResponse(BackendlessCollection<Setlist> setlists) {
                Log.i(Constants.LOG_TAG, "Handling setlist response");
                setlist = setlists.getCurrentPage().get(0).getSet();
                df.setTimeZone(TimeZone.getDefault());
                setStamp = "Updated:\n" + DateFormat.format(df.toLocalizedPattern(),
                        setlists.getCurrentPage().get(0).getUpdated());
                setlistWork(setlist, setStamp);
                intent.putExtra("success", true);
                context.sendBroadcast(intent);
            }

            @Override
            public void handleFault(BackendlessFault fault) {
                Log.e(Constants.LOG_TAG, "Error getting setlist!");
                setlist = "Error downloading setlist";
                setStamp = "";
                setlistWork(setlist, setStamp);
                intent.putExtra("success", false);
                context.sendBroadcast(intent);
            }
        });
    }

    /**
     * Create a string list, separating each line of the setlist to an
     * individual string
     */
    public static void parseSetlist(String setlist) {
        Log.i(Constants.LOG_TAG, "Parsing setlist");
        setlistList = new ArrayList<>(Arrays.asList(setlist.split("\n")));
        Log.i(Constants.LOG_TAG, setlistList.toString());
    }
    
    /**
     * Make the URI for the audio to add to the notification
     * @param soundId resource id of the audio
     */
    public static void createNotificationUri(Context context, int soundId) {
        String string = "android.resource://"
                .concat(context.getPackageName())
                .concat("/")
                .concat(Integer.toString(soundId));
        notificationSound = Uri.parse(string);
    }
    
    /**
     * Make the URI for the audio to add to the notification
     * @param soundPath path of the audio
     */
    public static void createNotificationUri(Context context, String soundPath) {
    	File file = new File(soundPath);
    	if (file.exists()) {
            notificationSound = Uri.parse(soundPath);
        } else {
            createNotificationUri(context, R.raw.general);
        }
    }
    
    /**
     * Create the map that associates song titles to images and audio for the
     * notifications
     * @param context
     */
    private static void generateSongMap(Context context) {
        songMap = new PatriciaTrie<>(StringKeyAnalyzer.CHAR);
        
        songMap.put("belly belly nice", new SongInfo(R.drawable.away_from_the_world, R.raw.aftw));
        songMap.put("belly full", new SongInfo(R.drawable.away_from_the_world, R.raw.aftw));
        songMap.put("broken things", new SongInfo(R.drawable.away_from_the_world, R.raw.aftw));
        songMap.put("drunken soldier", new SongInfo(R.drawable.away_from_the_world, R.raw.aftw));
        songMap.put("gaucho", new SongInfo(R.drawable.away_from_the_world, R.raw.aftw));
        songMap.put("if only", new SongInfo(R.drawable.away_from_the_world, R.raw.aftw));
        songMap.put("mercy", new SongInfo(R.drawable.away_from_the_world, R.raw.aftw));
        songMap.put("the riff", new SongInfo(R.drawable.away_from_the_world, R.raw.aftw));
        songMap.put("rooftop", new SongInfo(R.drawable.away_from_the_world, R.raw.aftw));
        songMap.put("snow outside", new SongInfo(R.drawable.away_from_the_world, R.raw.aftw));
        songMap.put("sweet", new SongInfo(R.drawable.away_from_the_world, R.raw.aftw));
        
        songMap.put("#35", new SongInfo(R.drawable.big_whiskey, R.raw.bw));
        songMap.put("alligator pie", new SongInfo(R.drawable.big_whiskey, R.raw.bw));
        songMap.put("baby blue", new SongInfo(R.drawable.big_whiskey, R.raw.bw));
        songMap.put("dive in", new SongInfo(R.drawable.big_whiskey, R.raw.bw));
        songMap.put("funny the way it is", new SongInfo(R.drawable.big_whiskey, R.raw.bw));
        songMap.put("grux", new SongInfo(R.drawable.big_whiskey, R.raw.bw));
        songMap.put("lying in the hands of god", new SongInfo(R.drawable.big_whiskey, R.raw.bw));
        songMap.put("seven", new SongInfo(R.drawable.big_whiskey, R.raw.bw));
        songMap.put("shake me like a monkey", new SongInfo(R.drawable.big_whiskey, R.raw.bw));
        songMap.put("spaceman", new SongInfo(R.drawable.big_whiskey, R.raw.bw));
        songMap.put("squirm", new SongInfo(R.drawable.big_whiskey, R.raw.bw));
        songMap.put("time bomb", new SongInfo(R.drawable.big_whiskey, R.raw.bw));
        songMap.put("why i am", new SongInfo(R.drawable.big_whiskey, R.raw.bw));
        songMap.put("you and me", new SongInfo(R.drawable.big_whiskey, R.raw.bw));
        
        songMap.put("american baby", new SongInfo(R.drawable.stand_up, R.raw.standup));
        songMap.put("american baby intro", new SongInfo(R.drawable.stand_up, R.raw.standup));
        songMap.put("dreamgirl", new SongInfo(R.drawable.stand_up, R.raw.standup));
        songMap.put("dream girl", new SongInfo(R.drawable.stand_up, R.raw.standup));
        songMap.put("everybody wake up", new SongInfo(R.drawable.stand_up, R.raw.standup));
        songMap.put("hello again", new SongInfo(R.drawable.stand_up, R.raw.standup));
        songMap.put("hunger for the great light", new SongInfo(R.drawable.stand_up, R.raw.standup));
        songMap.put("louisiana bayou", new SongInfo(R.drawable.stand_up, R.raw.standup));
        songMap.put("old dirt hill", new SongInfo(R.drawable.stand_up, R.raw.standup));
        songMap.put("out of my hands", new SongInfo(R.drawable.stand_up, R.raw.standup));
        songMap.put("smooth rider", new SongInfo(R.drawable.stand_up, R.raw.standup));
        songMap.put("stand up", new SongInfo(R.drawable.stand_up, R.raw.standup));
        songMap.put("steady as we go", new SongInfo(R.drawable.stand_up, R.raw.standup));
        songMap.put("stolen away on 55th & 3rd", new SongInfo(R.drawable.stand_up, R.raw.standup));
        songMap.put("you might die trying", new SongInfo(R.drawable.stand_up, R.raw.standup));
        
        songMap.put("an another thing", new SongInfo(R.drawable.some_devil, R.raw.somedevil));
        songMap.put("baby", new SongInfo(R.drawable.some_devil, R.raw.somedevil));
        songMap.put("dodo", new SongInfo(R.drawable.some_devil, R.raw.somedevil));
        songMap.put("gravedigger", new SongInfo(R.drawable.some_devil, R.raw.somedevil));
        songMap.put("grey blue eyes", new SongInfo(R.drawable.some_devil, R.raw.somedevil));
        songMap.put("oh", new SongInfo(R.drawable.some_devil, R.raw.somedevil));
        songMap.put("save me", new SongInfo(R.drawable.some_devil, R.raw.somedevil));
        songMap.put("so damn lucky", new SongInfo(R.drawable.some_devil, R.raw.somedevil));
        songMap.put("some devil", new SongInfo(R.drawable.some_devil, R.raw.somedevil));
        songMap.put("stay or leave", new SongInfo(R.drawable.some_devil, R.raw.somedevil));
        songMap.put("too high", new SongInfo(R.drawable.some_devil, R.raw.somedevil));
        songMap.put("trouble", new SongInfo(R.drawable.some_devil, R.raw.somedevil));
        songMap.put("up and away", new SongInfo(R.drawable.some_devil, R.raw.somedevil));
        
        songMap.put("bartender", new SongInfo(R.drawable.busted_stuff, R.raw.bs));
        songMap.put("big eyed fish", new SongInfo(R.drawable.busted_stuff, R.raw.bs));
        songMap.put("busted stuff", new SongInfo(R.drawable.busted_stuff, R.raw.bs));
        songMap.put("captain", new SongInfo(R.drawable.busted_stuff, R.raw.bs));
        songMap.put("digging a ditch", new SongInfo(R.drawable.busted_stuff, R.raw.bs));
        songMap.put("grace is gone", new SongInfo(R.drawable.busted_stuff, R.raw.bs));
        songMap.put("grey street", new SongInfo(R.drawable.busted_stuff, R.raw.bs));
        songMap.put("kit kat jam", new SongInfo(R.drawable.busted_stuff, R.raw.bs));
        songMap.put("raven", new SongInfo(R.drawable.busted_stuff, R.raw.bs));
        songMap.put("where are you going", new SongInfo(R.drawable.busted_stuff, R.raw.bs));
        songMap.put("you never know", new SongInfo(R.drawable.busted_stuff, R.raw.bs));
        
        songMap.put("angel", new SongInfo(R.drawable.everyday, R.raw.everyday));
        songMap.put("dreams of our fathers", new SongInfo(R.drawable.everyday, R.raw.everyday));
        songMap.put("everyday", new SongInfo(R.drawable.everyday, R.raw.everyday));
        songMap.put("fool to think", new SongInfo(R.drawable.everyday, R.raw.everyday));
        songMap.put("i did it", new SongInfo(R.drawable.everyday, R.raw.everyday));
        songMap.put("if i had it all", new SongInfo(R.drawable.everyday, R.raw.everyday));
        songMap.put("mother father", new SongInfo(R.drawable.everyday, R.raw.everyday));
        songMap.put("sleep to dream her", new SongInfo(R.drawable.everyday, R.raw.everyday));
        songMap.put("so right", new SongInfo(R.drawable.everyday, R.raw.everyday));
        songMap.put("the space between", new SongInfo(R.drawable.everyday, R.raw.everyday));
        songMap.put("what you are", new SongInfo(R.drawable.everyday, R.raw.everyday));
        songMap.put("when the world ends", new SongInfo(R.drawable.everyday, R.raw.everyday));
        
        songMap.put("crush", new SongInfo(R.drawable.before_these_crowded_streets, R.raw.btcs));
        songMap.put("dont drink the water",
                new SongInfo(R.drawable.before_these_crowded_streets, R.raw.btcs));
        songMap.put("dreaming tree", new SongInfo(R.drawable.before_these_crowded_streets, R.raw.btcs));
        songMap.put("halloween", new SongInfo(R.drawable.before_these_crowded_streets, R.raw.btcs));
        songMap.put("last stop", new SongInfo(R.drawable.before_these_crowded_streets, R.raw.btcs));
        songMap.put("pantala naga pampa",
                new SongInfo(R.drawable.before_these_crowded_streets, R.raw.btcs));
        songMap.put("pig", new SongInfo(R.drawable.before_these_crowded_streets, R.raw.btcs));
        songMap.put("rapunzel", new SongInfo(R.drawable.before_these_crowded_streets, R.raw.btcs));
        songMap.put("spoon", new SongInfo(R.drawable.before_these_crowded_streets, R.raw.btcs));
        songMap.put("stay", new SongInfo(R.drawable.before_these_crowded_streets, R.raw.btcs));
        songMap.put("stay wasting time", new SongInfo(R.drawable.before_these_crowded_streets, R.raw.btcs));
        songMap.put("the stone", new SongInfo(R.drawable.before_these_crowded_streets, R.raw.btcs));
        
        songMap.put("#41", new SongInfo(R.drawable.crash, R.raw.crash));
        songMap.put("crash into me", new SongInfo(R.drawable.crash, R.raw.crash));
        songMap.put("cry freedom", new SongInfo(R.drawable.crash, R.raw.crash));
        songMap.put("drive in drive out", new SongInfo(R.drawable.crash, R.raw.crash));
        songMap.put("let you down", new SongInfo(R.drawable.crash, R.raw.crash));
        songMap.put("lie in our graves", new SongInfo(R.drawable.crash, R.raw.crash));
        songMap.put("proudest monkey", new SongInfo(R.drawable.crash, R.raw.crash));
        songMap.put("say goodbye", new SongInfo(R.drawable.crash, R.raw.crash));
        songMap.put("so much to say", new SongInfo(R.drawable.crash, R.raw.crash));
        songMap.put("too much", new SongInfo(R.drawable.crash, R.raw.crash));
        songMap.put("tripping billies", new SongInfo(R.drawable.crash, R.raw.crash));
        songMap.put("two step", new SongInfo(R.drawable.crash, R.raw.crash));
        
        songMap.put("#34", new SongInfo(R.drawable.under_the_table_and_dreaming, R.raw.uttad));
        songMap.put("ants marching", new SongInfo(R.drawable.under_the_table_and_dreaming, R.raw.uttad));
        songMap.put("best of whats around",
                new SongInfo(R.drawable.under_the_table_and_dreaming, R.raw.uttad));
        songMap.put("dancing nancies", new SongInfo(R.drawable.under_the_table_and_dreaming, R.raw.uttad));
        songMap.put("jimi thing", new SongInfo(R.drawable.under_the_table_and_dreaming, R.raw.uttad));
        songMap.put("lover lay down", new SongInfo(R.drawable.under_the_table_and_dreaming, R.raw.uttad));
        songMap.put("pay for what you get",
                new SongInfo(R.drawable.under_the_table_and_dreaming, R.raw.uttad));
        songMap.put("rhyme and reason", new SongInfo(R.drawable.under_the_table_and_dreaming, R.raw.uttad));
        songMap.put("satellite", new SongInfo(R.drawable.under_the_table_and_dreaming, R.raw.uttad));
        songMap.put("typical situation",
                new SongInfo(R.drawable.under_the_table_and_dreaming, R.raw.uttad));
        songMap.put("warehouse", new SongInfo(R.drawable.under_the_table_and_dreaming, R.raw.uttad));
        songMap.put("what would you say",
                new SongInfo(R.drawable.under_the_table_and_dreaming, R.raw.uttad));
        
        songMap.put("christmas song", new SongInfo(R.drawable.remember_two_things, R.raw.r2t));
        songMap.put("ill back you up", new SongInfo(R.drawable.remember_two_things, R.raw.r2t));
        songMap.put("minarets", new SongInfo(R.drawable.remember_two_things, R.raw.r2t));
        songMap.put("one sweet world", new SongInfo(R.drawable.remember_two_things, R.raw.r2t));
        songMap.put("recently", new SongInfo(R.drawable.remember_two_things, R.raw.r2t));
        songMap.put("seek up", new SongInfo(R.drawable.remember_two_things, R.raw.r2t));
        songMap.put("the song that jane likes", new SongInfo(R.drawable.remember_two_things, R.raw.r2t));
        
        songMap.put("encore", new SongInfo(R.drawable.notification_large, R.raw.endofset));
        
        for (Entry<String, SongInfo> entry : songMap.entrySet()) {
        	String songName = entry.getKey();
        	SongInfo info = entry.getValue();
        	String songFile = StringUtils.remove(
        			StringUtils.remove(
        					StringUtils.remove(songName, " "), "'"), "#");
        	if (!DatabaseHelperSingleton.instance(context).hasNotificationSong(songFile)) {
                DatabaseHelperSingleton.instance(context).addNotification(songFile, info.getImage(),
                        info.getAudio());
            } else {
                DatabaseHelperSingleton.instance(context).updateNotification(songFile,
                        info.getImage(), info.getAudio());
            }
        }
    }
    
    /**
     * Get the image that matches the given song title for the notification
     *
     * @param context
     * @param songTitle    title of the song to match
     * @return id for the image resource that matches
     */
    public static int findMatchingImage(Context context, String songTitle) {
        songTitle = StringUtils.remove(songTitle, "*");
        songTitle = StringUtils.remove(songTitle, "+");
        songTitle = StringUtils.remove(songTitle, "~");
        songTitle = StringUtils.remove(songTitle, "�");
        songTitle = StringUtils.remove(songTitle, "#");
        songTitle = StringUtils.remove(songTitle, "-");
        songTitle = StringUtils.remove(songTitle, ">");
        songTitle = StringUtils.remove(songTitle, "(");
        songTitle = StringUtils.remove(songTitle, ")");
        songTitle = StringUtils.replace(songTitle, "’", "'");
        songTitle = StringUtils.strip(songTitle);
        songTitle = StringUtils.lowerCase(songTitle, Locale.ENGLISH);
        songTitle = StringUtils.remove(songTitle, " ");
    	songTitle = StringUtils.remove(songTitle, "'");
        return DatabaseHelperSingleton.instance(context).getNotificationImage(
        		songTitle);
    }
    
    /**
     * Get the audio that matches the current song for the notification.
     * @param songTitle	title of song to match
     */
    public static void findMatchingAudio(Context context, String songTitle) {
        songTitle = StringUtils.remove(songTitle, "*");
        songTitle = StringUtils.remove(songTitle, "+");
        songTitle = StringUtils.remove(songTitle, "~");
        songTitle = StringUtils.remove(songTitle, "�");
        songTitle = StringUtils.remove(songTitle, "#");
        songTitle = StringUtils.remove(songTitle, "-");
        songTitle = StringUtils.remove(songTitle, ">");
        songTitle = StringUtils.remove(songTitle, "(");
        songTitle = StringUtils.remove(songTitle, ")");
        songTitle = StringUtils.replace(songTitle, "’", "'");
        songTitle = StringUtils.strip(songTitle);
        songTitle = StringUtils.lowerCase(songTitle, Locale.ENGLISH);
        //songTitle = StringUtils.remove(songTitle, " ");
    	//songTitle = StringUtils.remove(songTitle, "'");
        Entry<String, SongInfo> entry = songMap.select(songTitle);
        Log.w(Constants.LOG_TAG, songTitle + " : " + entry.getKey());
        switch (SharedPreferencesSingleton.instance(context).getInt(
                ResourcesSingleton.instance(context).getString(R.string.notificationtype_key), 0)) {
        case 0:
        	ApplicationEx.createNotificationUri(context, R.raw.general);
        	break;
        case 1:
        	if (songTitle.startsWith(entry.getKey())) {
        		ApplicationEx.createNotificationUri(context, entry.getValue().getAudio());
        	} else {
        		ApplicationEx.createNotificationUri(context, R.raw.general);
        	}
        	break;
        /*
        case 2:
        	Log.i(Constants.LOG_TAG, "SONG AUDIO");
        	songTitle = StringUtils.remove(songTitle, " ");
        	songTitle = StringUtils.remove(songTitle, "'");
        	StringBuilder sb = new StringBuilder();
        	sb.append(cacheLocation);
        	sb.append(Constants.AUDIO_LOCATION);
        	sb.append(songTitle);
        	sb.append(".mp3");
        	ApplicationEx.createNotificationUri(sb.toString());
        	break;
        */
    	default:
    		ApplicationEx.createNotificationUri(context, R.raw.general);
        	break;
        }
    }
    
    /**
     * Resizes image for the large notification image.
     * @param res	resources object to retrieve the bitmap from
     * @param resId	id of the image to create bitmap from
     * @return bitmap to be used as the large notification image
     */
    @SuppressLint("InlinedApi")
	public static Bitmap resizeImage(Resources res, int resId) {
        Bitmap bitmap;
        try {
        	bitmap = BitmapFactory.decodeResource(res, resId);
        } catch (OutOfMemoryError e) {
        	return BitmapFactory.decodeResource(res, R.drawable.notification_large);
        }
        if (resId == R.drawable.notification_large)
            return bitmap;
        double ratio = (double)bitmap.getHeight() / (double)bitmap.getWidth();
        return Bitmap.createScaledBitmap(bitmap, res.getDimensionPixelSize(
                android.R.dimen.notification_large_icon_width), (int) (ratio < 1 ? res.getDimensionPixelSize(
                    android.R.dimen.notification_large_icon_height)*ratio : res.getDimensionPixelSize(
                        android.R.dimen.notification_large_icon_height)), true);
    }
    
    /**
     * Set the current drawable for login, question and stats background,
     * specific to the orientation.
     * @param backgroundDrawable	drawable for the current orientation
     */
    public static void setBackgroundBitmap(Context context, Bitmap backgroundDrawable) {
        switch(ResourcesSingleton.instance(context).getConfiguration().orientation) {
        case Configuration.ORIENTATION_PORTRAIT:
            ApplicationEx.portraitBackgroundBitmap = backgroundDrawable;
            break;
        case Configuration.ORIENTATION_LANDSCAPE:
            ApplicationEx.landBackgroundBitmap = backgroundDrawable;
            break;
        default:
            break;
        }
    }
    
    /**
     * Current drawable for the login, question and stats background.  Both
     * portrait and landscape are held here to reduce work when rotating.
     * @return current drawable for the login, question and stats background
     */
    public static Bitmap getBackgroundBitmap(Context context) {
        switch(ResourcesSingleton.instance(context).getConfiguration().orientation) {
        case Configuration.ORIENTATION_PORTRAIT:
            return ApplicationEx.portraitBackgroundBitmap;
        case Configuration.ORIENTATION_LANDSCAPE:
            return ApplicationEx.landBackgroundBitmap;
        default:
            return ApplicationEx.portraitBackgroundBitmap;
        }
    }
    
    /**
     * Set the current drawable for setlist background, specific to the
     * orientation.
     * @param setlistDrawable	drawable for the current orientation
     */
    public static void setSetlistBitmap(Context context, Bitmap setlistDrawable) {
        switch(ResourcesSingleton.instance(context).getConfiguration().orientation) {
        case Configuration.ORIENTATION_PORTRAIT:
            ApplicationEx.portraitSetlistBitmap = setlistDrawable;
            break;
        case Configuration.ORIENTATION_LANDSCAPE:
            ApplicationEx.landSetlistBitmap = setlistDrawable;
            break;
        default:
            break;
        }
    }
    
    /**
     * Current drawable for the setlist background.  Both portrait and landscape
     * are held here to reduce work when rotating.
     * @return current drawable for the setlist background
     */
    public static Bitmap getSetlistBitmap(Context context) {
        switch(ResourcesSingleton.instance(context).getConfiguration().orientation) {
        case Configuration.ORIENTATION_PORTRAIT:
            return ApplicationEx.portraitSetlistBitmap;
        case Configuration.ORIENTATION_LANDSCAPE:
            return ApplicationEx.landSetlistBitmap;
        default:
            return ApplicationEx.portraitSetlistBitmap;
        }
    }
    
    public static void downloadSongClips(List<String> songs) {
    	isDownloading = true;
    	SongDownloadTask songDownloadTask = new SongDownloadTask(songs);
        songDownloadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    
    private static class SongDownloadTask extends AsyncTask<Void, Void, Void> {
    	List<String> songs;
    	
    	private SongDownloadTask(List<String> songs) {
    		this.songs = songs;
    	}
    	
    	@Override
        protected Void doInBackground(Void... nothing) {
            // TODO Convert to Backendless if needed
            /**
    		FileOutputStream fout;
            File audioFile;
            StringBuilder sb = new StringBuilder();
    		ParseFile file;
    		ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Audio");
    		if (songs != null)
    			query.whereContainedIn("name", songs);
    		try {
				List<ParseObject> audioList = query.find();
				String name;
				for (ParseObject audio : audioList) {
					name = audio.getString("name");
					DatabaseHelperSingleton.instance()
							.songNotificationDownloaded(name, false);
					sb.setLength(0);
					sb.append(cacheLocation);
					sb.append(Constants.AUDIO_LOCATION);
					sb.append(name);
					sb.append(".mp3");
					audioFile = new File(sb.toString());
					file = (ParseFile) audio.get("file");
					try {
						fout = new FileOutputStream(audioFile);
						fout.write(file.getData());
						fout.flush();
						fout.close();
						DatabaseHelperSingleton.instance()
								.songNotificationDownloaded(name, true);
					} catch (ParseException e) {
						Log.e(Constants.LOG_TAG, "Couldn't get data from " +
								"ParseFile " + audio.getString("name"), e);
						if (e.getCode() == ParseException.CONNECTION_FAILED) {
							DatabaseHelperSingleton.instance()
									.songNotificationDownloaded(name, false);
						}
					} catch (FileNotFoundException e) {
						Log.e(Constants.LOG_TAG, sb.toString() + " not found!",
								e);
					} catch (IOException e) {
						Log.e(Constants.LOG_TAG, "Can't write to " +
								sb.toString(), e);
					}
	    		}
			} catch (ParseException e) {
				Log.e(Constants.LOG_TAG, "Couldn't find audio!", e);
			}
    		isDownloading = false;
             */
    		return null;
    	}
    }
    
    public static boolean isDownloading() {
    	return isDownloading;
    }
    
	public static float getTextViewHeight() {
		return textViewHeight;
	}
	
	public static void setTextViewHeight(float textViewHeight) {
		ApplicationEx.textViewHeight = textViewHeight;
	}
	
	public static List<String> getSerialsList() {
		return serialsList;
	}
	
	public static String getUpdatedDateString(long millis) {
        ApplicationEx.df.setTimeZone(TimeZone.getDefault());
        Date date = new Date();
        date.setTime(millis);
        return ApplicationEx.df.format(date.getTime());
    }

}