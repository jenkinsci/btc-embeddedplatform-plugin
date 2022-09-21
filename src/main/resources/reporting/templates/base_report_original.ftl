<#ftl encoding="utf-8">

<#macro page_head>
  <title>${page_title}</title>
<@page_main_style/>

</#macro>

<#macro page_main_style>
#addStyleHere
</#macro>


<#macro page_script>
#addScriptHere
</#macro>

<#macro page_head_addition>
  <!-- your head extras go here. -->
</#macro>

<#macro page_body_header_important>
  <!-- your header content goes here. Example: -->
  <h1>${page_title}</h1>
      <ul class="metainfo">
        <li><span>Creator:</span> ${creator}</li>
        <li><span>Creation date:</span> ${date}</li>
        <li style="text-align:right"><p class="buttonExpand"  id="expandCollapse">Expand/Collapse All</p></li>       
        <@page_head_metainfo_addition/>
      </ul>
</#macro>

<#macro page_head_metainfo_addition>
  <!-- your meta info <li> go here. -->
</#macro>

<#macro page_body_main>
  <!-- your body goes here. -->
  Base body intentionally left blank.
</#macro>

<#macro display_page>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8"/>
  <meta http-equiv="x-ua-compatible" content="ie=edge"/>
  <meta name="viewport" content="width=device-width, initial-scale=1"/>
  <@page_head/> 
  <@page_head_addition/> 
</head>
<body>
  <a name="#top"></a>
  <div class="wrapper">
    <header>
      <a class="logo"></a>
     <@page_body_header_important/>
    </header>

    <main>
    <@page_body_main/>
    </main>
  </div>
  <@page_script/>
</body>
</html>
</#macro>

<#function replaceNewlines i>
    <#return i
      ?replace("&","&amp;",'r')
      ?replace("\"","&quot;",'r')
      ?replace("'","&#39;",'r')
      ?replace("<","&lt;",'r')
      ?replace(">","&gt;",'r')
      ?replace("\\r?\\n","<br/>",'r')
      ?no_esc>
</#function>