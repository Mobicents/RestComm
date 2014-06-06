package org.mobicents.servlet.restcomm.rvd.packaging.model;

import org.mobicents.servlet.restcomm.rvd.validation.ValidatableModel;
import org.mobicents.servlet.restcomm.rvd.validation.ValidationReport;

/**
 * Restcomm application package model class
 * @author "Tsakiridis Orestis"
 *
 */
public class Rapp extends ValidatableModel {

    private RappInfo info;
    private RappConfig config;

    public RappInfo getInfo() {
        return info;
    }
    public RappConfig getConfig() {
        return config;
    }
    public Rapp setInfo(RappInfo info) {
        this.info = info;
        return this;
    }
    public Rapp setConfig(RappConfig config) {
        this.config = config;
        return this;
    }
    @Override
    public ValidationReport validate(ValidationReport report) {
        // Validate top level rapp fields
        if ( info == null )
            report.addErrorItem("Rapp info is missing (null)");
        if ( config == null )
            report.addErrorItem("Rapp config is missing (null)");

        // Validate child properties
        info.validate(report);
        config.validate(report);

        return report;
    }
}
