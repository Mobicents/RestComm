package org.mobicents.servlet.restcomm.rvd.upgrade;

import org.apache.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ProjectUpgrader714To10 extends ProjectUpgrader {
    static final Logger logger = Logger.getLogger(ProjectUpgrader714To10.class.getName());

    public ProjectUpgrader714To10() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Stub function to upgrade directly from json string instead of JsonElement
     */
    public JsonElement upgrade(String source) {
        JsonParser parser = new JsonParser();
        JsonElement sourceRoot = parser.parse(source);

        JsonElement targetRoot = this.upgrade(sourceRoot);

        return targetRoot;
    }

    /**
     * Upgrades a ProjectState JsonElement to the next version in the version path
     */
    public JsonElement upgrade(JsonElement sourceElement) {

        logger.info("Upgrading project from version rvd714 to 1.0");

        JsonObject source = sourceElement.getAsJsonObject();
        JsonObject target = new JsonObject();

        // root
        target.add("lastStepId", source.get("lastStepId"));
        target.add("lastNodeId", source.get("lastNodeId"));

        // root.iface
        JsonObject t = new JsonObject();
        t.addProperty("activeNode", 0);
        target.add("iface", t);

        // root.header
        t = new JsonObject();
        t.addProperty("projectKind", "voice"); // only voice project in rvd714
        t.addProperty("version", "1.0");
        t.add("startNodeName", source.get("startNodeName") );
        target.add("header", t);

        // root.nodes
        JsonArray tNodes = new JsonArray();
        for ( JsonElement sourceNode : source.getAsJsonArray("nodes") ) {
            JsonObject tNode = new JsonObject();
            JsonObject s = sourceNode.getAsJsonObject();

            tNode.add("name", s.get("name"));
            tNode.add("label", s.get("label"));
            tNode.addProperty("kind", "voice"); // only voice modules supported in rvd714
            tNode.add("iface", new JsonObject()); // put nothing in there. There should be no problem...

            // root.nodes.steps
            JsonObject sourceSteps = s.getAsJsonObject("steps");
            JsonArray targetSteps = new JsonArray();
            for ( JsonElement stepNameElement : s.getAsJsonArray("stepnames") ) {
                String stepName = stepNameElement.getAsString();

                JsonElement tStep = upgradeStep( sourceSteps.get(stepName) );
                targetSteps.add( tStep );
            }
            tNode.add("steps", targetSteps);

            tNodes.add(tNode);
        }
        target.add("nodes", tNodes);

        logger.debug(target.toString());

        return target;
    }

    private JsonElement upgradeStep(JsonElement sourceStep) {
        JsonObject o = sourceStep.getAsJsonObject();
        String kind =  o.get("kind").getAsString();
        if ( "say".equals(kind) ) {
            return upgradeSayStep(sourceStep);
        } else
        if ( "play".equals(kind) ) {
            return upgradePlayStep(sourceStep);
        } else
        if ( "gather".equals(kind) ) {
            return upgradeGatherStep(sourceStep);
        } else
        if ( "dial".equals(kind) ) {
            return upgradeDialStep(sourceStep);
        } else
        if ( "redirect".equals(kind) ) {
            return upgradeRedirectStep(sourceStep);
        }            

        return sourceStep;
    }

    private JsonElement upgradeSayStep(JsonElement sourceSay) {
        JsonObject o = sourceSay.getAsJsonObject();
        JsonObject t = new JsonObject();

        t.add("name", o.get("name"));
        t.add("kind", o.get("kind"));
        t.add("label", o.get("label"));
        t.add("title", o.get("title"));
        t.add("phrase", o.get("phrase"));
        if ( !o.get("voice").isJsonNull() && !o.get("voice").getAsString().equals("") )
            t.add("voice", o.get("voice"));
        if ( !o.get("loop").isJsonNull() )
            t.add("loop", o.get("loop"));
        if ( !o.get("language").isJsonNull() && !o.get("language").getAsString().equals("") )
            t.add("language", o.get("language"));
        t.add("iface", new JsonObject());

        return t;
    }
    
    private JsonElement upgradePlayStep(JsonElement sourceSay) {
        JsonObject o = sourceSay.getAsJsonObject();
        JsonObject t = new JsonObject();

        t.add("name", o.get("name"));
        t.add("kind", o.get("kind"));
        t.add("label", o.get("label"));
        t.add("title", o.get("title"));
        if ( !o.get("loop").isJsonNull() )
            t.add("loop", o.get("loop"));
        t.add("playType", o.get("playType"));
        
        String wavUrl = "";
        if ( o.get("wavUrl").getAsJsonPrimitive().isString() )
            wavUrl = o.get("wavUrl").getAsJsonPrimitive().getAsString();
        JsonObject remote = new JsonObject();
        remote.addProperty("wavUrl", wavUrl);
        t.add("remote", remote);
        
        String wavLocalFilename = "";
        if ( o.get("wavLocalFilename").getAsJsonPrimitive().isString() )
            wavLocalFilename = o.get("wavLocalFilename").getAsJsonPrimitive().getAsString();
        JsonObject local = new JsonObject();
        remote.addProperty("wavLocalFilename", wavLocalFilename);
        t.add("local", local);   
        
        t.add("iface", new JsonObject());

        return t;
    }   
    
    private JsonElement upgradeGatherStep(JsonElement source) {
        JsonObject o = source.getAsJsonObject();
        JsonObject t = new JsonObject();

        t.add("name", o.get("name"));
        t.add("kind", o.get("kind"));
        t.add("label", o.get("label"));
        t.add("title", o.get("title"));
        t.add("method", o.get("method"));
        if ( o.get("timeout").isJsonPrimitive() && o.get("timeout").getAsJsonPrimitive().isNumber() )
            t.add("timeout", o.get("timeout")); 
        if ( !o.get("finishOnKey").isJsonNull()  && !"".equals(o.get("finishOnKey").getAsString()) )
            t.add("finishOnKey", o.get("finishOnKey")); 
        if ( o.get("numDigits").isJsonPrimitive() && o.get("numDigits").getAsJsonPrimitive().isNumber() )
            t.add("numDigits", o.get("numDigits"));         
        
        JsonObject collectdigits = new JsonObject();
        collectdigits.add("next", o.get("next"));
        collectdigits.add("collectVariable", o.get("collectVariable"));
        collectdigits.addProperty("scope", "module");
        t.add("collectdigits", collectdigits);
        
        JsonObject menu = new JsonObject();
        menu.add("mappings", o.get("mappings"));
        
        JsonObject sourceSteps = o.getAsJsonObject("steps");
        JsonArray targetSteps = new JsonArray();
        for ( JsonElement stepNameElement : o.getAsJsonArray("stepnames") ) {
            String stepName = stepNameElement.getAsString();
            JsonElement tStep = upgradeStep( sourceSteps.get(stepName) );
            targetSteps.add( tStep );
        }
        t.add("steps", targetSteps);

        t.add("iface", new JsonObject());

        return t;
    }    

    private JsonElement upgradeDialStep(JsonElement sourceStep) {
        JsonObject o = sourceStep.getAsJsonObject();
        JsonObject t = new JsonObject();

        t.add("name", o.get("name"));
        t.add("kind", o.get("kind"));
        t.add("label", o.get("label"));
        t.add("title", o.get("title"));
        
        String dialType = o.get("dialType").getAsString();
        JsonObject noun = new JsonObject();
        if ( "number".equals(dialType) ) {
            noun.addProperty("dialType","number");
            noun.add("destination", o.get("number"));
        } else
        if ( "client".equals(dialType) ) {
            noun.addProperty("dialType","client");
            noun.add("destination", o.get("client"));
        } else
        if ( "conference".equals(dialType) ) {
            noun.addProperty("dialType","conference");
            noun.add("destination", o.get("conference"));
        } else      
        if ( "sipuri".equals(dialType) ) {
            noun.addProperty("dialType","sipuri");
            noun.add("destination", o.get("sipuri"));
        }
        JsonArray dialNouns = new JsonArray();
        dialNouns.add(noun);
        t.add("dialNouns", dialNouns);
        
        t.add("iface", new JsonObject());

        return t;
    }
    
    private JsonElement upgradeRedirectStep(JsonElement sourceStep) {
        JsonObject o = sourceStep.getAsJsonObject();
        JsonObject t = new JsonObject();

        t.add("name", o.get("name"));
        t.add("kind", o.get("kind"));
        t.add("label", o.get("label"));
        t.add("title", o.get("title"));
        
        String url = null;
        if ( o.get("url").isJsonPrimitive() && !"".equals(o.get("url").getAsString()) )
            url = o.get("url").getAsString();
        t.add("url", o.get(url));
        
        String method = null;
        if ( o.get("method").isJsonPrimitive() && !"".equals(o.get("method").getAsString()) )
            method = o.get("method").getAsString();
        t.add("method", o.get(method));       

        t.add("iface", new JsonObject());

        return t;
    }
    
    @Override
    public String getResultingVersion() {
        return "1.0";
    }

}
