package ru.mdm;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.mdm.businesslayer.BusinessComponent;
import ru.mdm.businesslayer.Customer;
import ru.mdm.integration.IntegrationMessage;
import ru.mdm.model.User;
import ru.mdm.steps.Esb;
import ru.mdm.utils.Utils;
import ru.mdm.xmlbuild.CustomerAddRqXmlBuilder;
import ru.mdm.xmlbuild.CustomerUpdateRqXmlBuilder;

import java.nio.charset.Charset;

/**
 * Created by Taborko Pavel on 07.02.2020
 */

@Slf4j
public class UpdateCustomerTest extends BaseClass {
    private CustomerAddRqType customerAddRqType;
    private CustomerUpdateRqType customerUpdateRqType;
    private String rqID;
    private String customerId;
    private IntegrationMessage integrationMessage;

    @BeforeAll
    static void init() throws Exception {
        connect(User.MDM_OPERATOR1);
    }

    @Test(description = "Тест 16. Обновление всех атрибутов записи со всеми заполненными атрибутами")
    public void test() throws Exception {
        editParametersForXml();
        sendXml();
        getResponse();
        editParametersForSecondXml();
        sendSecondXml();
        getSecondResponse();
        checkXsd();
        checkFields();
    }

    @Step("Определение необходимых параметров для xml типа CustomerAddRq")
    private CustomerAddRqType editParametersForXml() throws Exception {
        String xml = utils.Utils.readFile("src/test/resources/CustomerAddRq.xml", Charset.forName("UTF-8"));
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
        new IntegrationMessage(getMessageByXmlPart("Rs%" + rqID, Esb.Queue.ESB_MDM_ASBS_RESPONSE,
                "ASBS.CustomerAddRq").get().getXml());
    }

    @Step("Определение необходимых параметров для xml типа CustomerUpdateRq")
    private CustomerUpdateRqType editParametersForSecondXml() throws Exception {
        String xml = utils.Utils.readFile("src/test/resources/CustomerUpdateRq.xml", Charset.forName("UTF-8"));
        IntegrationMessage integrationMessage = new IntegrationMessage(xml);
        customerUpdateRqType = integrationMessage.getJaxb(CustomerUpdateRqType.class);
        rqID = Utils.generateRqID();
        CustomerUpdateRqXmlBuilder customerUpdateRqXmlBuilder = new CustomerUpdateRqXmlBuilder();
        return customerUpdateRqXmlBuilder.customerUpdateRqTypeBuilder(customerUpdateRqType, rqID, customerId);
    }

    @Step("Отправка xml типа CustomerUpdateRq")
    private void sendSecondXml() throws Exception {
        putMessage(new IntegrationMessage(editParametersForSecondXml()), Esb.Queue.ESB_MDM_ASBS_REQUEST);
    }

    @Step("Получение xml типа CustomerUpdateRs")
    private void getSecondResponse() throws Exception {
        integrationMessage = new IntegrationMessage(getMessageByXmlPart("Rs%" + rqID, Esb.Queue.ESB_MDM_ASBS_RESPONSE,
                "ASBS.CustomerUpdateRq").get().getXml());
    }

    @Step("Валидация сообщения по xsd")
    private void checkXsd() {
        validate(integrationMessage, "CustomerUpdateRs.xsd");
        softly.assertAll();
    }

    @Step("Поиск клиента и проверка полей")
    private void checkFields() {
        //Поиск клиента
        Customer customer = new Customer(getStand(), customerId);

        //Активация бизнес-компонентов
        BusinessComponent personalDataBC = customer.getChildBusComp("MDM Personal Data");
        BusinessComponent contactInformationBC = customer.getChildBusComp("MDM Contact Information");
        BusinessComponent customerAddressBC = customer.getChildBusComp("MDM Customer Address");
        BusinessComponent customerIdentityCardBC = customer.getChildBusComp("MDM Identity Card");
        BusinessComponent employmentHistoryBC = customer.getChildBusComp("MDM Employment History");
        BusinessComponent vehicleInformationBC = customer.getChildBusComp("MDM Vehicle Information");

        //Проверка соответствия значений тегов в xml сообщении типа CustomerUpdateRq и значений полей на бизнес-компонентах
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getPersonName().getLastName())
                .as("Неверное значение поля Фамилия").isEqualTo(personalDataBC.getFieldValue("Last Name"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getPersonName().getFirstName())
                .as("Неверное значение поля Имя").isEqualTo(personalDataBC.getFieldValue("First Name"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getPersonName().getMiddleName())
                .as("Неверное значение поля Отчество").isEqualTo(personalDataBC.getFieldValue("Middle Name"));

        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfContactData().getContactData().get(0).getContactType())
                .as("Неверное значение поля Тип контактной информации").isEqualTo(contactInformationBC.getFieldValue("Cell Phone"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfContactData().getContactData().get(0).getContactNum())
                .as("Неверное значение поля Значение").isEqualTo("+79037680102");
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfContactData().getContactData().get(0).getContactType())
                .as("Неверное значение поля Тип контактной информации").isEqualTo(contactInformationBC.getFieldValue("Cell Phone"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfContactData().getContactData().get(0).getContactNum())
                .as("Неверное значение поля Значение").isEqualTo("+79037680202");
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfContactData().getContactData().get(1).getContactType())
                .as("Неверное значение поля Тип контактной информации").isEqualTo(contactInformationBC.getFieldValue("Home Phone"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfContactData().getContactData().get(1).getContactNum())
                .as("Неверное значение поля Значение").isEqualTo("+74957540102");
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfContactData().getContactData().get(1).getContactType())
                .as("Неверное значение поля Тип контактной информации").isEqualTo(contactInformationBC.getFieldValue("Home Phone"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfContactData().getContactData().get(1).getContactNum())
                .as("Неверное значение поля Значение").isEqualTo("+74957540202");
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfContactData().getContactData().get(2).getContactType())
                .as("Неверное значение поля Тип контактной информации").isEqualTo(contactInformationBC.getFieldValue("Work Phone"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfContactData().getContactData().get(2).getContactNum())
                .as("Неверное значение поля Значение").isEqualTo("79037680103");
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfContactData().getContactData().get(2).getContactType())
                .as("Неверное значение поля Тип контактной информации").isEqualTo(contactInformationBC.getFieldValue("Work Phone"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfContactData().getContactData().get(2).getContactNum())
                .as("Неверное значение поля Значение").isEqualTo("79037680203");
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfContactData().getContactData().get(3).getContactType())
                .as("Неверное значение поля Тип контактной информации").isEqualTo(contactInformationBC.getFieldValue("Email"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfContactData().getContactData().get(3).getContactNum())
                .as("Неверное значение поля Значение").isEqualTo("ivanov@mail.ru");
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfContactData().getContactData().get(3).getContactType())
                .as("Неверное значение поля Тип контактной информации").isEqualTo(contactInformationBC.getFieldValue("Email"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfContactData().getContactData().get(3).getContactNum())
                .as("Неверное значение поля Значение").isEqualTo("petrov@mail.ru");

        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfAddress().getAddress().get(0).getAddressType())
                .as("Неверное значение поля Тип адреса").isEqualTo(customerAddressBC.getFieldValue("Registration Address"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfAddress().getAddress().get(0).getCountry())
                .as("Неверное значение поля Страна").isEqualTo(customerAddressBC.getFieldValue("Country"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfAddress().getAddress().get(0).getCity())
                .as("Неверное значение поля Город").isEqualTo(customerAddressBC.getFieldValue("City"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfAddress().getAddress().get(0).getStreet())
                .as("Неверное значение поля Улица").isEqualTo(customerAddressBC.getFieldValue("Street"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfAddress().getAddress().get(0).getHouseNum())
                .as("Неверное значение поля Дом").isEqualTo(customerAddressBC.getFieldValue("House Num"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfAddress().getAddress().get(0).getHouseExt())
                .as("Неверное значение поля Корпус").isEqualTo(customerAddressBC.getFieldValue("House Ext"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfAddress().getAddress().get(0).getUnit())
                .as("Неверное значение поля Строение").isEqualTo(customerAddressBC.getFieldValue("Unit"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfAddress().getAddress().get(0).getUnitNum())
                .as("Неверное значение поля Квартира/офис").isEqualTo(customerAddressBC.getFieldValue("Unit Num"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfAddress().getAddress().get(0).getAddressType())
                .as("Неверное значение поля Тип адреса").isEqualTo(customerAddressBC.getFieldValue("Registration Address"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfAddress().getAddress().get(0).getCountry())
                .as("Неверное значение поля Страна").isEqualTo(customerAddressBC.getFieldValue("Country"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfAddress().getAddress().get(0).getCity())
                .as("Неверное значение поля Город").isEqualTo(customerAddressBC.getFieldValue("City"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfAddress().getAddress().get(0).getStreet())
                .as("Неверное значение поля Улица").isEqualTo(customerAddressBC.getFieldValue("Street"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfAddress().getAddress().get(0).getHouseNum())
                .as("Неверное значение поля Дом").isEqualTo(customerAddressBC.getFieldValue("House Num"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfAddress().getAddress().get(0).getHouseExt())
                .as("Неверное значение поля Корпус").isEqualTo(customerAddressBC.getFieldValue("House Ext"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfAddress().getAddress().get(0).getUnit())
                .as("Неверное значение поля Строение").isEqualTo(customerAddressBC.getFieldValue("Unit"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfAddress().getAddress().get(0).getUnitNum())
                .as("Неверное значение поля Квартира/офис").isEqualTo(customerAddressBC.getFieldValue("Unit Num"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfAddress().getAddress().get(1).getAddressType())
                .as("Неверное значение поля Тип адреса").isEqualTo(customerAddressBC.getFieldValue("Residence Address"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfAddress().getAddress().get(1).getCountry())
                .as("Неверное значение поля Страна").isEqualTo(customerAddressBC.getFieldValue("Country"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfAddress().getAddress().get(1).getCity())
                .as("Неверное значение поля Город").isEqualTo(customerAddressBC.getFieldValue("City"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfAddress().getAddress().get(1).getStreet())
                .as("Неверное значение поля Улица").isEqualTo(customerAddressBC.getFieldValue("Street"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfAddress().getAddress().get(1).getHouseNum())
                .as("Неверное значение поля Дом").isEqualTo(customerAddressBC.getFieldValue("House Num"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfAddress().getAddress().get(1).getHouseExt())
                .as("Неверное значение поля Корпус").isEqualTo(customerAddressBC.getFieldValue("House Ext"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfAddress().getAddress().get(1).getUnit())
                .as("Неверное значение поля Строение").isEqualTo(customerAddressBC.getFieldValue("Unit"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfAddress().getAddress().get(1).getUnitNum())
                .as("Неверное значение поля Квартира/офис").isEqualTo(customerAddressBC.getFieldValue("Unit Num"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfAddress().getAddress().get(1).getAddressType())
                .as("Неверное значение поля Тип адреса").isEqualTo(customerAddressBC.getFieldValue("Residence Address"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfAddress().getAddress().get(1).getCountry())
                .as("Неверное значение поля Страна").isEqualTo(customerAddressBC.getFieldValue("Country"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfAddress().getAddress().get(1).getCity())
                .as("Неверное значение поля Город").isEqualTo(customerAddressBC.getFieldValue("City"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfAddress().getAddress().get(1).getStreet())
                .as("Неверное значение поля Улица").isEqualTo(customerAddressBC.getFieldValue("Street"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfAddress().getAddress().get(1).getHouseNum())
                .as("Неверное значение поля Дом").isEqualTo(customerAddressBC.getFieldValue("House Num"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfAddress().getAddress().get(1).getHouseExt())
                .as("Неверное значение поля Корпус").isEqualTo(customerAddressBC.getFieldValue("House Ext"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfAddress().getAddress().get(1).getUnit())
                .as("Неверное значение поля Строение").isEqualTo(customerAddressBC.getFieldValue("Unit"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfAddress().getAddress().get(1).getUnitNum())
                .as("Неверное значение поля Квартира/офис").isEqualTo(customerAddressBC.getFieldValue("Unit Num"));

        softly.assertThat(customerUpdateRqType.getCustomerInfo().getBirthDate())
                .as("Неверное значение поля Дата рождения").isEqualTo(personalDataBC.getFieldValue("Birth Date"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getBirthPlace())
                .as("Неверное значение поля Место рождения").isEqualTo(personalDataBC.getFieldValue("Birth Place"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getCitizenShip())
                .as("Неверное значение поля Гражданство").isEqualTo(personalDataBC.getFieldValue("Citizenship"));

        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(0).getIdType())
                .as("Неверное значение поля Тип документа").isEqualTo(customerIdentityCardBC.getFieldValue("Passport Of Russia"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(0).getIdSeries())
                .as("Неверное значение поля Серия документа").isEqualTo("4513");
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(0).getIdNum())
                .as("Неверное значение поля Номер документа").isEqualTo("123456");
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(0).getIssueDt())
                .as("Неверное значение поля Дата выдачи документа").isEqualTo(customerIdentityCardBC.getFieldValue("Issue Data"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(0).getIssuedBy())
                .as("Неверное значение поля Кем выдан документ").isEqualTo(customerIdentityCardBC.getFieldValue("Issued By"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(0).getCode())
                .as("Неверное значение поля Код подразделения, выдавшего ДУЛ").isEqualTo(customerIdentityCardBC.getFieldValue("Code"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(0).getLastName())
                .as("Неверное значение поля Фамилия").isEqualTo(customerIdentityCardBC.getFieldValue("Last Name"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(0).getFirstName())
                .as("Неверное значение поля Имя").isEqualTo(customerIdentityCardBC.getFieldValue("First Name"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(0).getMiddleName())
                .as("Неверное значение поля Отчество").isEqualTo(customerIdentityCardBC.getFieldValue("Middle Name"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(0).getIdType())
                .as("Неверное значение поля Тип документа").isEqualTo(customerIdentityCardBC.getFieldValue("Passport Of Russia"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(0).getIdSeries())
                .as("Неверное значение поля Серия документа").isEqualTo("4514");
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(0).getIdNum())
                .as("Неверное значение поля Номер документа").isEqualTo("654321");
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(0).getIssueDt())
                .as("Неверное значение поля Дата выдачи документа").isEqualTo(customerIdentityCardBC.getFieldValue("Issue Data"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(0).getIssuedBy())
                .as("Неверное значение поля Кем выдан документ").isEqualTo(customerIdentityCardBC.getFieldValue("Issued By"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(0).getCode())
                .as("Неверное значение поля Код подразделения, выдавшего ДУЛ").isEqualTo(customerIdentityCardBC.getFieldValue("Code"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(0).getLastName())
                .as("Неверное значение поля Фамилия").isEqualTo(customerIdentityCardBC.getFieldValue("Last Name"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(0).getFirstName())
                .as("Неверное значение поля Имя").isEqualTo(customerIdentityCardBC.getFieldValue("First Name"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(0).getMiddleName())
                .as("Неверное значение поля Отчество").isEqualTo(customerIdentityCardBC.getFieldValue("Middle Name"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(1).getIdType())
                .as("Неверное значение поля Тип документа").isEqualTo(customerIdentityCardBC.getFieldValue("International Passport Of Russia"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(1).getIdSeries())
                .as("Неверное значение поля Серия документа").isEqualTo("11");
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(1).getIdNum())
                .as("Неверное значение поля Номер документа").isEqualTo("2494799");
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(1).getIssueDt())
                .as("Неверное значение поля Дата выдачи документа").isEqualTo(customerIdentityCardBC.getFieldValue("Issue Data"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(1).getIssuedBy())
                .as("Неверное значение поля Кем выдан документ").isEqualTo(customerIdentityCardBC.getFieldValue("Issued By"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(1).getCode())
                .as("Неверное значение поля Код подразделения, выдавшего ДУЛ").isEqualTo(customerIdentityCardBC.getFieldValue("Code"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(1).getLastName())
                .as("Неверное значение поля Фамилия").isEqualTo(customerIdentityCardBC.getFieldValue("Last Name"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(1).getFirstName())
                .as("Неверное значение поля Имя").isEqualTo(customerIdentityCardBC.getFieldValue("First Name"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(1).getMiddleName())
                .as("Неверное значение поля Отчество").isEqualTo(customerIdentityCardBC.getFieldValue("Middle Name"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(1).getIdType())
                .as("Неверное значение поля Тип документа").isEqualTo(customerIdentityCardBC.getFieldValue("International Passport Of Russia"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(1).getIdSeries())
                .as("Неверное значение поля Серия документа").isEqualTo("12");
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(1).getIdNum())
                .as("Неверное значение поля Номер документа").isEqualTo("2494788");
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(1).getIssueDt())
                .as("Неверное значение поля Дата выдачи документа").isEqualTo(customerIdentityCardBC.getFieldValue("Issue Data"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(1).getIssuedBy())
                .as("Неверное значение поля Кем выдан документ").isEqualTo(customerIdentityCardBC.getFieldValue("Issued By"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(1).getCode())
                .as("Неверное значение поля Код подразделения, выдавшего ДУЛ").isEqualTo(customerIdentityCardBC.getFieldValue("Code"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(1).getLastName())
                .as("Неверное значение поля Фамилия").isEqualTo(customerIdentityCardBC.getFieldValue("Last Name"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(1).getFirstName())
                .as("Неверное значение поля Имя").isEqualTo(customerIdentityCardBC.getFieldValue("First Name"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(1).getMiddleName())
                .as("Неверное значение поля Отчество").isEqualTo(customerIdentityCardBC.getFieldValue("Middle Name"));

        softly.assertThat(customerUpdateRqType.getCustomerInfo().getSnils())
                .as("Неверное значение поля СНИЛС").isEqualTo(customerIdentityCardBC.getFieldValue("SNILS"));

        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfEmploymentHistory().getEmploymentHistory().get(0).getLegalName())
                .as("Неверное значение поля Работодатель").isEqualTo(employmentHistoryBC.getFieldValue("Employer"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfEmploymentHistory().getEmploymentHistory().get(0).getJob())
                .as("Неверное значение поля Должность").isEqualTo(employmentHistoryBC.getFieldValue("Position"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfEmploymentHistory().getEmploymentHistory().get(0).getLegalName())
                .as("Неверное значение поля Работодатель").isEqualTo(employmentHistoryBC.getFieldValue("Employer"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfEmploymentHistory().getEmploymentHistory().get(0).getJob())
                .as("Неверное значение поля Должность").isEqualTo(employmentHistoryBC.getFieldValue("Position"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfEmploymentHistory().getEmploymentHistory().get(1).getLegalName())
                .as("Неверное значение поля Работодатель").isEqualTo(employmentHistoryBC.getFieldValue("Employer"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfEmploymentHistory().getEmploymentHistory().get(1).getJob())
                .as("Неверное значение поля Должность").isEqualTo(employmentHistoryBC.getFieldValue("Position"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfEmploymentHistory().getEmploymentHistory().get(1).getLegalName())
                .as("Неверное значение поля Работодатель").isEqualTo(employmentHistoryBC.getFieldValue("Employer"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfEmploymentHistory().getEmploymentHistory().get(1).getJob())
                .as("Неверное значение поля Должность").isEqualTo(employmentHistoryBC.getFieldValue("Position"));

        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfVehicleInfo().getVehicleInfo().get(0).getVehicleBrand())
                .as("Неверное значение поля Марка ТС").isEqualTo(vehicleInformationBC.getFieldValue("Brand"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfVehicleInfo().getVehicleInfo().get(0).getVehicleNum())
                .as("Неверное значение поля Регистрационный номер ТС").isEqualTo(vehicleInformationBC.getFieldValue("Registration Number"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfVehicleInfo().getVehicleInfo().get(0).getVehicleAge())
                .as("Неверное значение поля Возраст ТС в годах").isEqualTo(vehicleInformationBC.getFieldValue("Vehicle Age"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfVehicleInfo().getVehicleInfo().get(0).getPurchaseYear())
                .as("Неверное значение поля Год приобретения").isEqualTo(vehicleInformationBC.getFieldValue("Purchase Year"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfVehicleInfo().getVehicleInfo().get(0).getMarketPrice())
                .as("Неверное значение поля Рыночная цена").isEqualTo(vehicleInformationBC.getFieldValue("Market Price"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfVehicleInfo().getVehicleInfo().get(0).getVehicleBrand())
                .as("Неверное значение поля Марка ТС").isEqualTo(vehicleInformationBC.getFieldValue("Brand"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfVehicleInfo().getVehicleInfo().get(0).getVehicleNum())
                .as("Неверное значение поля Регистрационный номер ТС").isEqualTo(vehicleInformationBC.getFieldValue("Registration Number"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfVehicleInfo().getVehicleInfo().get(0).getVehicleAge())
                .as("Неверное значение поля Возраст ТС в годах").isEqualTo(vehicleInformationBC.getFieldValue("Vehicle Age"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfVehicleInfo().getVehicleInfo().get(0).getPurchaseYear())
                .as("Неверное значение поля Год приобретения").isEqualTo(vehicleInformationBC.getFieldValue("Purchase Year"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfVehicleInfo().getVehicleInfo().get(0).getMarketPrice())
                .as("Неверное значение поля Рыночная цена").isEqualTo(vehicleInformationBC.getFieldValue("Market Price"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfVehicleInfo().getVehicleInfo().get(1).getVehicleBrand())
                .as("Неверное значение поля Марка ТС").isEqualTo(vehicleInformationBC.getFieldValue("Brand"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfVehicleInfo().getVehicleInfo().get(1).getVehicleNum())
                .as("Неверное значение поля Регистрационный номер ТС").isEqualTo(vehicleInformationBC.getFieldValue("Registration Number"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfVehicleInfo().getVehicleInfo().get(1).getVehicleAge())
                .as("Неверное значение поля Возраст ТС в годах").isEqualTo(vehicleInformationBC.getFieldValue("Vehicle Age"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfVehicleInfo().getVehicleInfo().get(1).getPurchaseYear())
                .as("Неверное значение поля Год приобретения").isEqualTo(vehicleInformationBC.getFieldValue("Purchase Year"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfVehicleInfo().getVehicleInfo().get(1).getMarketPrice())
                .as("Неверное значение поля Рыночная цена").isEqualTo(vehicleInformationBC.getFieldValue("Market Price"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfVehicleInfo().getVehicleInfo().get(1).getVehicleBrand())
                .as("Неверное значение поля Марка ТС").isEqualTo(vehicleInformationBC.getFieldValue("Brand"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfVehicleInfo().getVehicleInfo().get(1).getVehicleNum())
                .as("Неверное значение поля Регистрационный номер ТС").isEqualTo(vehicleInformationBC.getFieldValue("Registration Number"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfVehicleInfo().getVehicleInfo().get(1).getVehicleAge())
                .as("Неверное значение поля Возраст ТС в годах").isEqualTo(vehicleInformationBC.getFieldValue("Vehicle Age"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfVehicleInfo().getVehicleInfo().get(1).getPurchaseYear())
                .as("Неверное значение поля Год приобретения").isEqualTo(vehicleInformationBC.getFieldValue("Purchase Year"));
        softly.assertThat(customerUpdateRqType.getCustomerInfo().getListOfVehicleInfo().getVehicleInfo().get(1).getMarketPrice())
                .as("Неверное значение поля Рыночная цена").isEqualTo(vehicleInformationBC.getFieldValue("Market Price"));
        softly.assertAll();
    }
}