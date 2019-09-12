package com.company;

import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.common.DfException;

/**
 * @author Rafał Hiszpański
 */
public interface IQueryHandler {
    void process(IDfCollection row) throws DfException;
}
