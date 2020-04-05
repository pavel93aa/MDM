package ru.mdm;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.mdm.businesslayer.BusinessComponent;
import ru.mdm.businesslayer.Customer;
import ru.mdm.integration.IntegrationMessage;
import ru.mdm.integration.jaxb.dev.customerAddRq.customerAddRqType;
import ru.mdm.model.User;
import ru.mdm.steps.Esb;
import ru.mdm.utils.Utils;
import ru.mdm.xmlbuild.CustomerAddRqXmlBuilder;

import javax.xml.bind.JAXBException;
import java.nio.charset.Charset;

/**
 * Created by Taborko Pavel on 06.02.2020
 */

@Slf4j
public class CreateCustomerWithIncorrectInternationalPassportTest extends BaseClass {
    private CustomerAddRqType customerAddRqType;
    private String rqID;
    private String customerId;
    private IntegrationMessage integrationMessage;

    @BeforeAll
    static void init() throws Exception {
        connect(User.MDM_OPERATOR1);
    }

    @Test(description = "Тест 12. Создание записи с некорректным загранпаспортом РФ")
    public void test() throws Exception {
        editParametersForXml();
        sendXml();
        getResponse();
        checkXsd();
        checkFields();
    }

    @Step("Определение необходимых параметров для xml типа CustomerAddRq")
    private CustomerAddRqType editParametersForXml() throws Exception {
        String xml = utils.Utils.readFile("src/test/resources/CustomerAddRqWithIncorrectInternationalPassport.xml", Charset.forName("UTF-8"));
        IntegrationMessage integrationMessage = new IntegrationMessage(xml);
        customerAddRqType = integrationMessage.getJaxb(CustomerAddRqType.class);
        rqID = Utils.generateRqID();
        customerId = Utils.getRandomNumber(8);
        CustomerAddRqXmlBuilder customerAddRqXmlBuilder = new CustomerAddRqXmlBuilder();
        return customerAddRqXmlBuilder.customerAddRqTypeBuilder(customerAddRqType, rqID, customerId);
    }

    @Step("Отправка xml типа CustomerAddRq")
    private void sendXml() throws Exception {
        putMessage(new IntegrationMessage(editParametersForXml()), Esb.Queue.ESB_MDM_ASBS_REQUEST);
    }

    @Step("Получение xml типа CustomerAddRs")
    private void getResponse() throws Exception {
        integrationMessage = new IntegrationMessage(getMessageByXmlPart("Rs%" + rqID, Esb.Queue.ESB_MDM_ASBS_RESPONSE,
                "ASBS.CustomerAddRq").get().getXml());
    }

    @Step("Валидация сообщения по xsd")
    private void checkXsd() {
        validate(integrationMessage, "CustomerAddRs.xsd");
        softly.assertAll();
    }

    @Step("Поиск клиента и проверка полей")
    private void checkFields() {
        //Поиск клиента
        Customer customer = new Customer(getStand(), customerId);

        //Активация бизнес-компонентов
        BusinessComponent personalDataBC = customer.getChildBusComp("MDM Personal Data");
        BusinessComponent customerIdentityCardBC = customer.getChildBusComp("MDM Identity Card");

        //Проверка соответствия значений тегов в xml сообщении типа CustomerAddRq и значений полей на бизнес-компонентах
        softly.assertThat(customerAddRqType.getCustomerInfo().getPersonName().getLastName())
                .as("Неверное значение поля Фамилия").isEqualTo(personalDataBC.getFieldValue("Last Name"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getPersonName().getFirstName())
                .as("Неверное значение поля Имя").isEqualTo(personalDataBC.getFieldValue("First Name"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getPersonName().getMiddleName())
                .as("Неверное значение поля Отчество").isEqualTo(personalDataBC.getFieldValue("Middle Name"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getBirthDate())
                .as("Неверное значение поля Дата рождения").isEqualTo(personalDataBC.getFieldValue("Birth Date"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(0).getIdType())
                .as("Неверное значение поля Тип документа").isEqualTo(customerIdentityCardBC.getFieldValue("Passport Of Russia"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(0).getIdSeries())
                .as("Неверное значение поля Серия документа").isEqualTo("4513");
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(0).getIdNum())
                .as("Неверное значение поля Номер документа").isEqualTo("123456");
        softly.assertThat(customerIdentityCardBC.getFieldValue("International Passport Of Russia"))
                .as("Неверное значение поля Тип документа").isEmpty();
        softly.assertAll();
    }
}