<%@ include file="/WEB-INF/jsp/header.jsp" %>

<span class="info">Spaces</span>

<a href="<c:url value='/flow/space'/>">[ Create New Space ]</a>

<p/>

<table class="jtrac">

    <tr>
        <th>Space</th>
        <th>Edit</th>
        <th>Description</th>
        <th>Users</th>
    </tr>

    <c:forEach items="${spaces}" var="space" varStatus="row">
        <c:set var="rowClass">
            <c:choose>
                <c:when test="${selectedSpaceId == space.id}">class="selected"</c:when>
                <c:when test="${row.count % 2 == 0}">class="alt"</c:when>
            </c:choose>            
        </c:set>
        <tr ${rowClass}>
            <td>${space.prefixCode}</td>
            <td>
                <a href="<c:url value='/flow/space?spaceId=${space.id}'/>">(edit)</a>
            </td>
            <td>${space.description}</td>
            <td align="center">
                <a href="<c:url value='/flow/space_allocate?spaceId=${space.id}'/>">(+)</a>
            </td>
        </tr>
    </c:forEach>

</table>

<%@ include file="/WEB-INF/jsp/footer.jsp" %>