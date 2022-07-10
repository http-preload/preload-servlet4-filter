<%@page contentType="text/html; charset=UTF-8" session="false" %>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8" />
<title>Preload Example</title>
<link rel="stylesheet" href="/assets/index.css" />
</head>
<body>

<noscript>This page won't work in Preview mode</noscript>
<div id="app">Loading</div>
<%
response.flushBuffer();
try{
  Thread.sleep(10);
}catch(InterruptedException e){
  e.printStackTrace();
}
%>
<script type="module" src="/src/foobar.js"></script>
<script nomodule="">
document.getElementById('app').textContent = 'This page won\'t work in browsers which don\'t support ECMAScript module';
</script>

</body>
</html>
