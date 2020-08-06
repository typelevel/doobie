<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:outline="http://wkhtmltopdf.org/outline"
                xmlns="http://www.w3.org/1999/xhtml">
  <xsl:output doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
              doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"
              indent="yes" />
  <xsl:template match="outline:outline">
    <html>
      <head>
        <title>Table of Contents</title>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
        <link rel="stylesheet" type="text/css" href="file:///opt/paradoxsite/lib/foundation/dist/foundation.min.css"/>
        <link rel="stylesheet" type="text/css" href="file:///opt/paradoxsite/css/page.css"/>
        <link rel="stylesheet" type="text/css" href="file:///opt/paradoxsite/css/single.css"/>
        <link rel="stylesheet" type="text/css" href="file:///opt/paradoxsite/css/print.css"/>
      </head>
      <body>
        <h1>Table of Contents</h1>
        <nav class="print">
          <xsl:apply-templates select="outline:item/outline:item">
            <xsl:with-param name="depth" select="1"/>
          </xsl:apply-templates>
        </nav>
      </body>
    </html>
  </xsl:template>

  <xsl:template match="outline:item">
    <xsl:param name="depth"/>
    <xsl:comment>Only add the li tags for elements beyond the first depth, so that the roots don't get numbers from our css</xsl:comment>
    <xsl:choose>
      <xsl:when test="$depth &gt; 1">
        <li>
          <xsl:call-template name="item-inner">
            <xsl:with-param name="depth" select="$depth"/>
          </xsl:call-template>
        </li>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="item-inner">
          <xsl:with-param name="depth" select="$depth"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="item-inner">
    <xsl:param name="depth"/>
    <xsl:if test="@title!=''">
      <div class="toc-page-line">
        <a>
          <xsl:if test="@link">
            <xsl:attribute name="href"><xsl:value-of select="@link"/></xsl:attribute>
          </xsl:if>
          <xsl:if test="@backLink">
            <xsl:attribute name="name"><xsl:value-of select="@backLink"/></xsl:attribute>
          </xsl:if>
          <xsl:value-of select="@title" />
        </a>
        <span class="toc-page-no"> <xsl:value-of select="@page" /> </span>
      </div>
    </xsl:if>
    <xsl:if test="$depth &lt; 5">
      <ul>
        <xsl:comment>added to prevent self-closing tags in QtXmlPatterns</xsl:comment>
        <xsl:apply-templates select="outline:item">
          <xsl:with-param name="depth" select="$depth + 1"/>
        </xsl:apply-templates>
      </ul>
    </xsl:if>
  </xsl:template>
</xsl:stylesheet>
