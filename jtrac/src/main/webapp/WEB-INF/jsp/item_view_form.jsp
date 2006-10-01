<%@ include file="/WEB-INF/jsp/header.jsp" %>

<c:if test="${!empty calledBySearch}">
    <a href="<c:url value='/flow?_flowExecutionKey=${flowExecutionKey}&_eventId=back&itemId=${item.id}#goto'/>">(back)</a>
    <br/>
    <br/>
</c:if>

<jtrac:itemview item="${item}"/>

<c:if test="${principal.id > 0}">

<br/>

<form method="post" action="<c:url value='/flow'/>" enctype="multipart/form-data">

<table class="bdr-collapse" width="100%">

<tr>
<td>    
    
<table class="jtrac">
        
    <c:forEach items="${editableFields}" var="field">
        <tr>
            <td class="label">
                ${field.label}            
            </td>
            <spring:bind path="history.${field.name}">
                <%@ include file="/WEB-INF/jsp/item_form_include.jsp" %>               
            </spring:bind>
        </tr>        
    </c:forEach>    
    
    <tr>
        <td class="label">New Status</td>
        <td>
            <spring:bind path="history.status">
                <select name="${status.expression}">
                    <option/>
                    <c:forEach items="${transitions}" var="transitionEntry">
                        <option value="${transitionEntry.key}" <c:if test='${transitionEntry.key == status.value}'>selected="true"</c:if>>${transitionEntry.value}</option>
                    </c:forEach>
                </select>
                <span class="error">${status.errorMessage}</span>
            </spring:bind>
        </td>        
    </tr>
    <tr>
        <td class="label">Assign To</td>       
        <td>
            <spring:bind path="history.assignedTo">
                <select name="${status.expression}">
                    <option/>
                    <c:forEach items="${users}" var="user">
                        <option value="${user.id}" <c:if test='${user.id == status.value}'>selected="true"</c:if>>${user.name}</option>
                    </c:forEach>  
                </select>
                <span class="error">${status.errorMessage}</span>
            </spring:bind> 
        </td>        
    </tr>
     <tr>
        <td class="label">
            Comment
            <font color="red">*</font></td>
        <td>
            <spring:bind path="history.comment">
                <textarea name="${status.expression}" rows="6" cols="70">${status.value}</textarea>
                <div class="error">${status.errorMessage}</div>
            </spring:bind>
        </td>        
    </tr>
     <tr>
        <td/>
        <td>
            <input type="submit" name="_eventId_submit" value="Submit"/>
            <input type="checkbox" name="sendNotifications" value="true" <c:if test="${history.sendNotifications}">checked="true"</c:if>/>
            send e-mail notifications
            <input type="hidden" name="_sendNotifications"/>            
        </td>        
    </tr>  
</table>

</td>

<td valign="top">
    <table class="jtrac">
        <tr><th>Notify By E-mail</th></tr>
        <tr>
            <td>
                <spring:bind path="history.itemUsers">
                    <jtrac:multiselect name="${status.expression}" list="${users}" selected="${status.value}"/>
                </spring:bind>
            </td>
        </tr>
        <tr><th>Attachment</th></tr>
        <tr><td><input type="file" name="file"/></td></tr>
    </table>
</td>

</tr>

</table>

<input type="hidden" name="itemId" value="${item.id}"/>
<input type="hidden" name="_flowExecutionKey" value="${flowExecutionKey}"/>

</form>

</c:if>

<%@ include file="/WEB-INF/jsp/footer.jsp" %>