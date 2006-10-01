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

package info.jtrac.webflow;

import info.jtrac.domain.Attachment;
import info.jtrac.domain.History;
import info.jtrac.domain.Item;
import info.jtrac.domain.ItemItem;
import info.jtrac.domain.ItemUser;
import info.jtrac.domain.Space;
import info.jtrac.domain.State;
import info.jtrac.domain.User;
import info.jtrac.util.AttachmentUtils;
import info.jtrac.util.ItemUserEditor;
import info.jtrac.util.UserEditor;
import info.jtrac.util.ValidationUtils;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.acegisecurity.context.SecurityContextHolder;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.validation.DataBinder;
import org.springframework.validation.Errors;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.webflow.Event;
import org.springframework.webflow.RequestContext;
import org.springframework.webflow.ScopeType;
import org.springframework.webflow.context.servlet.ServletExternalContext;

/**
 * Multiaction that participates in the "Space Create / Edit" flow
 * for editing individual Fields
 */
public class ItemViewFormAction extends AbstractFormAction {
    
    public ItemViewFormAction() {
        setFormObjectClass(History.class);
        setFormObjectName("history");        
        setFormObjectScope(ScopeType.REQUEST);        
    }
    
    @Override
    protected void initBinder(RequestContext request, DataBinder binder) {
        binder.registerCustomEditor(Integer.class, new CustomNumberEditor(Integer.class, true));
        binder.registerCustomEditor(Double.class, new CustomNumberEditor(Double.class, true));
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
        binder.registerCustomEditor(Date.class, new CustomDateEditor(new SimpleDateFormat("yyyy-MM-dd"), true));
        binder.registerCustomEditor(User.class, new UserEditor(jtrac));
        binder.registerCustomEditor(ItemUser.class, new ItemUserEditor(jtrac));
    }    
    
    @Override
    public Object loadFormObject(RequestContext context) {
        Item item = null;
        String itemId = ValidationUtils.getParameter(context, "itemId");
        String relatedItemRefId = ValidationUtils.getParameter(context, "relatedItemRefId");
        if (itemId != null) {            
            item = jtrac.loadItem(Long.parseLong(itemId));            
        } else {
            item = (Item) context.getRequestScope().get("item");
        }
        List<User> users = jtrac.findUsersForSpace(item.getSpace().getId());
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Space space = item.getSpace();
        context.getRequestScope().put("transitions", item.getPermittedTransitions(user));
        context.getRequestScope().put("editableFields", item.getEditableFieldList(user));
        // not flow scope because of weird Hibernate Lazy loading issues
        // hidden field "itemId" added to item_view_form.jsp
        context.getRequestScope().put("item", item);
        context.getRequestScope().put("users", users);
        History history = new History();
        if (relatedItemRefId != null) {
            String relationType = ValidationUtils.getParameter(context, "relationType");
            context.getRequestScope().put("relatedItemRefId", relatedItemRefId);
            context.getRequestScope().put("relationType", relationType);
            String relationString = null;
            int type = Integer.parseInt(relationType);
            context.getRequestScope().put("relationText", ItemItem.getRelationText(type));
        }        
        history.setItemUsers(item.getItemUsers());
        return history;
    }     
    
    public Event itemViewHandler(RequestContext context) throws Exception {
        History history = (History) getFormObject(context);
        Errors errors = getFormErrors(context);
        if (history.getStatus() != null) {
            if (history.getStatus() != State.CLOSED && history.getAssignedTo() == null) {
                errors.rejectValue("assignedTo", "error.history.assignedTo.required", "Required if Status other than Closed.");
            }
        } else {
            if (history.getAssignedTo() != null) {
                errors.rejectValue("status", "error.history.status.required", "Required if assigning.");
            }
        }
        if (history.getComment() == null) {
            errors.rejectValue("comment", ValidationUtils.ERROR_EMPTY_CODE, ValidationUtils.ERROR_EMPTY_MSG);
        }
        if (errors.hasErrors()) {
            return error();
        }
        Item item = (Item) context.getRequestScope().get("item"); // loaded by loadFormObject() on submit
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();        
        history.setLoggedBy(user);
        
        // attachment handling
        ServletExternalContext servletContext = (ServletExternalContext) context.getLastEvent().getSource();
        MultipartHttpServletRequest request = (MultipartHttpServletRequest) servletContext.getRequest();
        MultipartFile multipartFile = request.getFile("file");
        Attachment attachment = null;
        if (!multipartFile.isEmpty()) {
            String fileName = AttachmentUtils.cleanFileName(multipartFile.getOriginalFilename());
            attachment = new Attachment();
            attachment.setFileName(fileName);
        }        
        
        // related item handling
        String refId = ValidationUtils.getParameter(context, "relatedItemRefId");
        if (refId != null) {
            String relationType = ValidationUtils.getParameter(context, "relationType");
            int pos = refId.indexOf('-');
            long sequenceNum = Long.parseLong(refId.substring(pos + 1));
            String prefixCode = refId.substring(0, pos).toUpperCase();
            Item relatedItem = jtrac.loadItem(sequenceNum, prefixCode);
            ItemItem itemItem = new ItemItem(relatedItem, Integer.parseInt(relationType));
            item.add(itemItem);
        }         
        
        jtrac.storeHistoryForItem(item, history, attachment);
        
        if (attachment != null) {
            File file = new File(System.getProperty("jtrac.home") + "/attachments/" + attachment.getFilePrefix() + "_" + attachment.getFileName());
            multipartFile.transferTo(file);
        }
        resetForm(context);
        return success();
    }
    
    public Event relateHandler(RequestContext context) {
        // there may be a better way to do this within the flow definition file
        // but this is a "marker" for switching on the "back" hyperlink
        // see the "input-mapper" sections in WEB-INF/flow/item_view.xml and item_search.xml
        context.getRequestScope().put("calledByRelate", true);
        String itemId = ValidationUtils.getParameter(context, "itemId");      
        Item item = jtrac.loadItem(Long.parseLong(itemId));
        context.getRequestScope().put("item", item);
        return success();
    }     
    
}
