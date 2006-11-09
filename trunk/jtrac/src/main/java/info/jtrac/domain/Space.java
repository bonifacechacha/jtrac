/*
 * Copyright 2002-2005 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.jtrac.domain;

import java.io.Serializable;

import static info.jtrac.domain.Field.Name.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A JTrac installation can be divided into different project
 * areas or workspaces.  The Space entity represents this concept.
 * The Metdata of a Space determines the type of
 * Items contained within the space.  Users can be mapped to a
 * space with different access permissions.
 */
public class Space implements Serializable {
    
    private long id;
    private int version;
    private Integer type;
    private String prefixCode;
    private String name;
    private String description;
    private boolean guestAllowed;
    private SpaceSequence spaceSequence = new SpaceSequence();
    private Metadata metadata = new Metadata();    
    
    //=======================================================
    
    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }     
    
    public SpaceSequence getSpaceSequence() {
        return spaceSequence;
    }

    public void setSpaceSequence(SpaceSequence spaceSequence) {
        this.spaceSequence = spaceSequence;
    }    
    
    public String getPrefixCode() {
        return prefixCode;
    }

    public void setPrefixCode(String prefixCode) {
        this.prefixCode = prefixCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }    
    
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }        
    
    public boolean isGuestAllowed() {
        return guestAllowed;
    }

    public void setGuestAllowed(boolean guestAllowed) {
        this.guestAllowed = guestAllowed;
    }    
    
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("id [").append(id);
        sb.append("]; prefixCode [").append(prefixCode);
        sb.append("]");
        return sb.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Space)) {
            return false;
        }
        final Space s = (Space) o;
        return prefixCode.equals(s.getPrefixCode());
    }
    
    @Override
    public int hashCode() {
        return prefixCode.hashCode();
    }
    
}