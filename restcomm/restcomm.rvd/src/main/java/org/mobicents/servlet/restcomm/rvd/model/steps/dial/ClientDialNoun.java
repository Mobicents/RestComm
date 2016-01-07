package org.mobicents.servlet.restcomm.rvd.model.steps.dial;

import org.mobicents.servlet.restcomm.rvd.exceptions.InterpreterException;
import org.mobicents.servlet.restcomm.rvd.interpreter.Interpreter;
import org.mobicents.servlet.restcomm.rvd.utils.RvdUtils;

import java.util.HashMap;
import java.util.Map;

public class ClientDialNoun extends DialNoun {
    private String destination;
    private String beforeConnectModule;

    public String getDestination() {
        return destination;
    }
    public String getBeforeConnectModule() {
        return beforeConnectModule;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    @Override
    public RcmlNoun render(Interpreter interpreter) throws InterpreterException {
        RcmlClientNoun rcmlNoun = new RcmlClientNoun();

        if ( ! RvdUtils.isEmpty(getBeforeConnectModule()) ) {
            Map<String, String> pairs = new HashMap<String, String>();
            pairs.put("target", getBeforeConnectModule());
            rcmlNoun.setUrl( interpreter.buildAction(pairs) );
        }

        rcmlNoun.setDestination( interpreter.populateVariables(getDestination()) );
        return rcmlNoun;
    }
}
