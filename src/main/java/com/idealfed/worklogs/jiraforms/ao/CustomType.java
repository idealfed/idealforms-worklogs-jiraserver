package com.idealfed.worklogs.jiraforms.ao;

import net.java.ao.Entity;

import net.java.ao.Preload;
import net.java.ao.schema.StringLength;

@Preload
public interface CustomType extends Entity
{

    String getName();
    void setName(String name);

    String getDescription();
    void setDescription(String description);

    String getFieldName();
    void setFieldName(String fieldName);

    String getCustomType();
    void setCustomType(String customType);


    @StringLength(value=StringLength.UNLIMITED)
    String getSettings();
    void setSettings(String settings);


}
