/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jtstand;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import org.jboss.logging.Logger;

/**
 *
 * @author albert
 */
public class TestFixtureBindings implements Bindings {

    private static final Logger log = Logger.getLogger(TestFixtureBindings.class.getName());
    private transient TestFixture testFixture;
    private transient Map<String, Object> localVariablesMap = new HashMap<String, Object>();

    public TestFixtureBindings() {
    }

    public TestFixtureBindings(TestFixture testFixture) {
        this.testFixture = testFixture;
    }

    @Override
    public Object put(String key, Object variableValue) {
        log.trace("put key: '" + key + "', value: '" + variableValue + "'");
        for (TestFixtureProperty tsp : testFixture.getProperties()) {
            if (tsp.getName().equals(key)) {
                if ((tsp.isFinal() == null || tsp.isFinal()) && testFixture.containsKey(key)) {
                    throw new IllegalStateException("Cannot change final variable: '" + key + "'");
                }
                return testFixture.put(key, variableValue);
            }
        }
        TestStation testStation = testFixture.getTestStation();
        if (testStation != null) {
            for (TestStationProperty tsp : testStation.getProperties()) {
                if (tsp.getName().equals(key)) {
                    if ((tsp.isFinal() == null || tsp.isFinal()) && testStation.containsKey(key)) {
                        throw new IllegalStateException("Cannot change final variable: '" + key + "'");
                    }
                    return testStation.put(key, variableValue);
                }
            }
            TestProject testProject = testStation.getTestProject();
            if (testProject != null) {
                for (TestProjectProperty tsp : testProject.getProperties()) {
                    if (tsp.getName().equals(key)) {
                        if ((tsp.isFinal() == null || tsp.isFinal()) && testStation.containsKey(key)) {
                            throw new IllegalStateException("Cannot change final variable: '" + key + "'");
                        }
                        return testStation.put(key, variableValue);
                    }
                }
            }
        }
        return localVariablesMap.put(key, variableValue);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> toMerge) {
        for (Entry<? extends String, ? extends Object> variableEntry : toMerge.entrySet()) {
            put(variableEntry.getKey(), variableEntry.getValue());
        }
    }

    @Override
    public boolean containsKey(Object key) {
        return testFixture.containsKey(key.toString())
                || "fixture".equals(key)
                || localVariablesMap.containsKey(key.toString())
                || testFixture.containsProperty(key.toString());
    }

    @Override
    public Object get(Object key) {
        log.trace("get key: '" + key + "'...");
        if ("context".equals(key)) {
            return ScriptContext.ENGINE_SCOPE;
        }
        if ("fixture".equals(key)) {
            return testFixture;
        }
        if ("station".equals(key)) {
            return testFixture.getTestStation();
        }

        if (localVariablesMap.containsKey((String) key)) {
            return localVariablesMap.get((String) key);
        }
        try {
            return testFixture.getVariable((String) key);
        } catch (ScriptException ex) {
            log.warn("ScriptException", ex);
            throw new IllegalStateException(ex.getMessage());
        } catch (InterruptedException ex) {
            throw new IllegalStateException(ex.getMessage());
        }
    }

    @Override
    public Object remove(Object key) {
        return localVariablesMap.remove((String) key);
    }

    @Override
    public int size() {
        return keySet().size();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        for (String key : keySet()) {
            if (value.equals(get(key))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void clear() {
        log.trace("clear() is called.");
        testFixture.clear();
        localVariablesMap.clear();
    }

    @Override
    public Set<String> keySet() {
        log.trace("keySet() is called.");
        Set<String> keys = new HashSet<String>();
        keys.addAll(testFixture.keySet());
        TestStation station = testFixture.getTestStation();
        if (station != null) {
            keys.addAll(station.keySet());
        }
        keys.add("fixture");
        keys.add("station");
        keys.addAll(localVariablesMap.keySet());
        return keys;
    }

    @Override
    public Collection<Object> values() {
        Collection<Object> values = new ArrayList<Object>();
        for (String key : keySet()) {
            values.add(get(key));
        }
        return values;
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        Set<Entry<String, Object>> entries = new HashSet<Entry<String, Object>>();
        for (String key : keySet()) {
            entries.add(new HashMap.SimpleEntry(key, get(key)));
        }
        return entries;
    }
}
