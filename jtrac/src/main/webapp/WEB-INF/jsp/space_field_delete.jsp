<%@ include file="/WEB-INF/jsp/header.jsp" %>

<span class="info">Confirm Field Delete: '${fieldForm.field.label}' [${fieldForm.field.name.description} : ${fieldForm.field.name.text}]</span>

<p/>

<form method="post" action="<c:url value='/flow'/>">

    <p>Are you sure that you want to delete this field?</p>
    <p>No of affected database records = ${affectedCount}</p>
    <span class="error">You cannot undo database updates for this operation.</span>
    <input type="submit" name="_eventId_submit" value="Submit"/>
    
    <p/>
        
    <input type="submit" name="_eventId_cancel" value="Cancel"/>
    <input type="hidden" name="_flowExecutionKey" value="${flowExecutionKey}"/>
    <input type="hidden" name="fieldName" value="${fieldForm.field.name.text}"/>
    
</form>

<%@ include file="/WEB-INF/jsp/footer.jsp" %>