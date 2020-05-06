package com.example.taxitmapp;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name = "response", strict = false)
class ResponseXML {
    @Element(required = false, name="descr")
    String descr;
}
