package org.restcomm.connect.rvd.model.steps.dial;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class NumberNounConverter implements Converter {

    @Override
    public boolean canConvert(Class elementClass) {
        return elementClass.equals(RcmlNumberNoun.class);
    }

    @Override
    public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
        RcmlNumberNoun step = (RcmlNumberNoun) value;

        if (step.getSendDigits() != null)
            writer.addAttribute("sendDigits", step.getSendDigits());
        if (step.getUrl() != null)
            writer.addAttribute("url", step.getUrl());
        writer.setValue(step.getDestination());
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader arg0, UnmarshallingContext arg1) {
        // TODO Auto-generated method stub
        return null;
    }

}
