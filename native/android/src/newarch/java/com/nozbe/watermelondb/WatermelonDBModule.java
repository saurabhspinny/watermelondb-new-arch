package com.nozbe.watermelondb;

import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import java.util.Map;
import java.util.HashMap;

public class WatermelonDBModule extends ReactContextBaseJavaModule {
    public static final String NAME = "WatermelonDB";

    WatermelonDBModule(ReactApplicationContext context) {
        super(context);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @ReactMethod
    public void initialize(final Integer tag, final String databaseName, final int schemaVersion, final boolean unsafeNativeReuse, Callback successCallback, Callback errorCallback) {
        try {
            if (connections.containsKey(tag)) {
                throw new IllegalStateException("A driver with tag " + tag + " already set up");
            }
            WritableMap promiseMap = Arguments.createMap();
            connections.put(tag, new Connection.Connected(new WMDatabaseDriver(reactContext, databaseName, schemaVersion, unsafeNativeReuse)));
            promiseMap.putString("code", "ok");
            successCallback.invoke(promiseMap);
        } catch (SchemaNeededError e) {
            WritableMap promiseMap = Arguments.createMap();
            connections.put(tag, new Connection.Waiting(new ArrayList<>()));
            promiseMap.putString("code", "schema_needed");
            successCallback.invoke(promiseMap);
        } catch (MigrationNeededError e) {
            WritableMap promiseMap = Arguments.createMap();
            connections.put(tag, new Connection.Waiting(new ArrayList<>()));
            promiseMap.putString("code", "migrations_needed");
            promiseMap.putInt("databaseVersion", e.databaseVersion);
            successCallback.invoke(promiseMap);
        } catch (Exception e) {
            errorCallback.invoke(e.getMessage());
        }
    }

    // Helper method for connecting the driver
    private void connectDriver(int connectionTag, WMDatabaseDriver driver, Callback successCallback, Callback errorCallback) {
        try {
            List<Runnable> queue = getQueue(connectionTag);
            connections.put(connectionTag, new Connection.Connected(driver));

            for (Runnable operation : queue) {
                operation.run();
            }
            successCallback.invoke(true);
        } catch (Exception e) {
            errorCallback.invoke(e.getMessage());
        }
    }

    // Helper method for disconnecting the driver
    private void disconnectDriver(int connectionTag) {
        List<Runnable> queue = getQueue(connectionTag);

        connections.remove(connectionTag);

        for (Runnable operation : queue) {
            operation.run();
        }
    }

    private List<Runnable> getQueue(int connectionTag) {
        List<Runnable> queue;
        if (connections.containsKey(connectionTag)) {
            Connection connection = connections.get(connectionTag);
            if (connection != null) {
                queue = connection.getQueue();
            } else {
                queue = new ArrayList<>();
            }
        } else {
            queue = new ArrayList<>();
        }
        return queue;
    }

    @ReactMethod
    public void setUpWithSchema(final Integer tag, final String databaseName, final String schema, final int schemaVersion, final boolean unsafeNativeReuse, Callback successCallback, Callback errorCallback) {
        try {
            connectDriver(tag, new WMDatabaseDriver(reactContext, databaseName, new Schema(schemaVersion, schema), unsafeNativeReuse), successCallback, errorCallback);
        } catch (Exception e) {
            errorCallback.invoke(e.getMessage());
        }
    }

    @ReactMethod
    public void setUpWithMigrations(final Integer tag, final String databaseName, final String migrations, final int fromVersion, final int toVersion, final boolean unsafeNativeReuse, Callback successCallback, Callback errorCallback) {
        try {
            connectDriver(tag, new WMDatabaseDriver(reactContext, databaseName, new MigrationSet(fromVersion, toVersion, migrations), unsafeNativeReuse), successCallback, errorCallback);
        } catch (Exception e) {
            disconnectDriver(tag);
            errorCallback.invoke(e.getMessage());
        }
    }

    @ReactMethod
    private void find(int tag, String table, String id, Callback successCallback, Callback errorCallback) {
        withDriver(tag, successCallback, errorCallback, 
            (driver) -> driver.find(table, id), 
            "find " + id
        );
    }


    @ReactMethod
    public void query(int tag, String table, String query, ReadableArray args, Callback successCallback, Callback errorCallback) {
        withDriver(tag, successCallback, errorCallback, 
            (driver) -> driver.cachedQuery(table, query, args.toArrayList().toArray()), 
            "query"
        );
    }

    @ReactMethod
    public void queryIds(int tag, String query, ReadableArray args, Callback successCallback, Callback errorCallback) {
        withDriver(tag, successCallback, errorCallback, 
            (driver) -> {
                List<String> ids = driver.queryIds(query, args.toArrayList().toArray());
                WritableArray result = Arguments.createArray();
                for (String id : ids) {
                    result.pushString(id);
                }
                return result;
            }, 
            "queryIds"
        );
    }


    @ReactMethod
    public void unsafeQueryRaw(int tag, String query, ReadableArray args, Callback successCallback, Callback errorCallback) {
        withDriver(tag, successCallback, errorCallback, (driver) -> {
            List<Object> rawResults = driver.unsafeQueryRaw(query, args.toArrayList().toArray());
            WritableArray result = Arguments.createArray();
            for (Object item : rawResults) {
                result.pushString(item.toString());
            }
        }, "unsafeQueryRaw");
    }

    @ReactMethod
    public void count(int tag, String query, ReadableArray args, Callback successCallback, Callback errorCallback) {
        withDriver(tag, successCallback, errorCallback, 
            (driver) -> driver.count(query, args.toArrayList().toArray()), 
            "count"
        );
    }


    @ReactMethod
    public void batch(int tag, ReadableArray operations, Callback successCallback, Callback errorCallback) {
        withDriver(tag, successCallback, errorCallback, (driver) -> {
            driver.batch(operations);
        }, "batch");
    }

    @ReactMethod
    public void unsafeResetDatabase(int tag, String schema, int schemaVersion, Callback successCallback, Callback errorCallback) {
        withDriver(tag, successCallback, errorCallback, (driver) -> {
            driver.unsafeResetDatabase(new Schema(schemaVersion, schema));
        }, "unsafeResetDatabase");
    }

    @ReactMethod
    public void getLocal(int tag, String key, Callback successCallback, Callback errorCallback) {
        withDriver(tag, successCallback, errorCallback, (driver) -> {
            String value = driver.getLocal(key);
        }, "getLocal");
    }

    @ReactMethod(isBlockingSynchronousMethod = true)
    public WritableArray unsafeGetLocalSynchronously(int tag, String key) {
        try {
            Connection connection = connections.get(tag);
            if (connection == null) {
                throw new Exception("No driver with tag " + tag + " available");
            }
            if (connection instanceof Connection.Connected) {
                String value = ((Connection.Connected) connection).driver.getLocal(key);
                WritableArray result = Arguments.createArray();
                result.pushString("result");
                result.pushString(value);
                return result;
            } else if (connection instanceof Connection.Waiting) {
                throw new Exception("Waiting connection unexpected for unsafeGetLocalSynchronously");
            }
        } catch (Exception e) {
            WritableArray result = Arguments.createArray();
            result.pushString("error");
            result.pushString(e.getMessage());
            return result;
        }
        return null;
    }

    @ReactMethod
    public void provideSyncJson(int id, String json, Callback successCallback, Callback errorCallback) {
        try {
            // Note: Reflection-based call to optional WatermelonJSI
            Class<?> clazz = Class.forName("com.nozbe.watermelondb.jsi.WatermelonJSI");
            Method method = clazz.getDeclaredMethod("provideSyncJson", int.class, byte[].class);
            method.invoke(null, id, json.getBytes());
            successCallback.invoke(true);
        } catch (Exception e) {
            errorCallback.invoke(e.getMessage());
        }
    }

    @ReactMethod(isBlockingSynchronousMethod = true)
    public WritableArray getRandomBytes(int count) {
        if (count != 256) {
            throw new IllegalStateException("Expected getRandomBytes to be called with 256");
        }

        byte[] randomBytes = new byte[256];
        SecureRandom random = new SecureRandom();
        random.nextBytes(randomBytes);

        WritableArray result = Arguments.createArray();
        for (byte value : randomBytes) {
            result.pushInt(Byte.toUnsignedInt(value));
        }
        return result;
    }

    @ReactMethod
    public void install(final Callback successCallback, final Callback errorCallback) {
        try {
            // Attempt to install JSI bindings
            JavaScriptContextHolder jsContext = getReactApplicationContext().getJavaScriptContextHolder();
            JSIInstaller.install(getReactApplicationContext(), jsContext.get());
            Log.i(NAME, "Successfully installed Watermelon DB JSI Bindings!");
            successCallback.invoke(true)
        } catch (Exception e) {
            Log.e(NAME, "Failed to install Watermelon DB JSI Bindings!", e);
            errorCallback.invoke("INSTALL_FAILED", "Failed to install JSI Bindings", e);
        }
    }

    interface ParamFunction {
        Object applyParamFunction(WMDatabaseDriver arg);
    }

    private void withDriver(final int tag, final Callback successCallback, final Callback errorCallback, final ParamFunction function, String functionName) {
        try {
            Trace.beginSection("WMDatabaseBridge." + functionName);
            Connection connection = connections.get(tag);

            if (connection == null) {
                errorCallback.invoke("No driver with tag " + tag + " available");
            } else if (connection instanceof Connection.Connected) {
                Object result = function.applyParamFunction(((Connection.Connected) connection).driver);
                successCallback.invoke(result == Void.TYPE ? true : result);
            } else if (connection instanceof Connection.Waiting) {
                // Try again when the driver is ready
                connection.getQueue().add(() -> withDriver(tag, successCallback, errorCallback, function, functionName));
                connections.put(tag, new Connection.Waiting(connection.getQueue()));
            }
        } catch (Exception e) {
            errorCallback.invoke(e.getMessage());
        } finally {
            Trace.endSection();
        }
    }

}