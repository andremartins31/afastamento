<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<header class="clearfix">
	<span>Afastamentos</span>
</header>
<div class="main">
	<nav class="cbp-hsmenu-wrapper" id="cbp-hsmenu-wrapper">
		<div class="cbp-hsinner">
			<ul class="cbp-hsmenu">
				<li>
					<a href="<c:url value="/" />" title="Novo Projeto"><i class="fa fa-plus-circle fa-2x"></i>&nbsp; Home</a>
				</li>
				<li style="float: right;">
					<a href="<c:url value="/j_spring_security_logout" />" title="Sair"><i class="fa fa-power-off"></i>&nbsp;</a>
				</li>
			</ul>
		</div>
		<div class="cbp-hsmenubg"></div>
	</nav>
</div>