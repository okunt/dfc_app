package com.company;

import com.documentum.fc.client.DfQuery;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;

import java.util.HashMap;

public class TypeToExtensionMapper {

    private HashMap<String, String> map;

    public TypeToExtensionMapper(IDfSession session) throws DfException {
        map = new HashMap<>();
        String dql = "select distinct a_content_type, dos_extension from dm_document,dm_format where a_content_type = name;";
        IDfQuery query = new DfQuery();
        query.setDQL(dql);
        IDfCollection coll = query.execute(session, IDfQuery.DF_READ_QUERY);

        while (coll.next()) {
            System.out.println(coll.getString("a_content_type") + " " + coll.getString("dos_extension"));
            map.put(coll.getString("a_content_type"),coll.getString("dos_extension"));
        }
        if (coll != null)
            coll.close();

        if(map.isEmpty()) throw new DfException("Mapper is empty");
    }

    String getExtension(String objectType) { return map.get(objectType); }
}
