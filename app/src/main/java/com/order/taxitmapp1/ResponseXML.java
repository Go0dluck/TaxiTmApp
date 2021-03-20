package com.order.taxitmapp1;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

@Root(name = "response", strict = false)
class ResponseXML {
    @Element(name="descr")
    String descr;

    @Path("data")
    @Element(required = false, name="DISCOUNTEDSUMM")
    String DISCOUNTEDSUMM;

    @Path("data")
    @Element(required = false, name="CLIENT_BONUS_BALANCE")
    String CLIENT_BONUS_BALANCE;
}
