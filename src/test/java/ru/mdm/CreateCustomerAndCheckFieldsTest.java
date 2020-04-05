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

import java.nio.charset.Charset;

/**
 * Created by Taborko Pavel on 07.02.2020
 */

@Slf4j
public class CreateCustomerAndCheckFieldsTest extends BaseClass {
    private CustomerAddRqType customerAddRqType;
    private String rqID;
    private String customerId;
    private IntegrationMessage integrationMessage;

    @BeforeAll
    static void init() throws Exception {
        browser = new Browser("test");
        connect(User.MDM_ADMIN);
    }

    @Test(description = "Тест 21. UI в Internet Explorer 11")
    public void test() throws Exception {
        editParametersForXml();
        sendXml();
        getResponse();
        checkXsd();
        checkFieldsInUI();
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
        integrationMessage = new IntegrationMessage(getMessageByXmlPart("Rs%" + rqID, Esb.Queue.ESB_MDM_ASBS_RESPONSE,
                "ASBS.CustomerAddRq").get().getXml());
    }

    @Step("Валидация сообщения по xsd")
    private void checkXsd() {
        validate(integrationMessage, "CustomerAddRs.xsd");
        softly.assertAll();
    }

    @Step("Поиск клиента и проверка полей в UI")
    private void checkFieldsInUI() {
        Customer customer = new Customer(getStand(), customerId);

        //Активация бизнес-компонентов
        BusinessComponent personalDataBC = customer.getChildBusComp("MDM Personal Data");
        BusinessComponent contactInformationBC = customer.getChildBusComp("MDM Contact Information");
        BusinessComponent customerAddressBC = customer.getChildBusComp("MDM Customer Address");
        BusinessComponent customerIdentityCardBC = customer.getChildBusComp("MDM Identity Card");
        BusinessComponent employmentHistoryBC = customer.getChildBusComp("MDM Employment History");
        BusinessComponent vehicleInformationBC = customer.getChildBusComp("MDM Vehicle Information");

        browser.login(User.MDM_ADMIN.getLogin());
        browser.goToView("MDM Home View");
        ListApplet listApplet = browser.onListApplet("MDM Home Applet");
        listApplet.control("CustomerSearch").click();
        listApplet = browser.onListApplet("MDM Customer Search Applet");

        //Проверка поиска клиента по уникальному идентификатору
        softly.assertThat(listApplet.control("SearchByUCI").isExist())
                .as("Поле Поиск по УИК не найдено").isTrue();
        softly.assertThat(listApplet.control("Search").isExist())
                .as("Кнопка Найти не найдена").isTrue();

        //Проверка поиска клиента по ФИО и дате рождения
        softly.assertThat(listApplet.control("LastName").isExist())
                .as("Поле Фамилия не найдено").isTrue();
        softly.assertThat(listApplet.control("FirstName").isExist())
                .as("Поле Имя не найдено").isTrue();
        softly.assertThat(listApplet.control("MiddleName").isExist())
                .as("Поле Отчество не найдено").isTrue();
        softly.assertThat(listApplet.control("RadioButtonBirthDate").isExist())
                .as("Переключатель Дата рождения не найден").isTrue();
        softly.assertThat(listApplet.control("RadioButtonYearOfBirth").isExist())
                .as("Переключатель Год рождения не найден").isTrue();
        softly.assertThat(listApplet.control("JDatePickBirthDate").isExist())
                .as("Поле Дата рождения не найдено").isTrue();
        softly.assertThat(listApplet.control("YearOfBirth").isExist())
                .as("Поле Год рождения не найдено").isTrue();

        //Проверка поиска клиента по документу, удостоверяющему личность
        softly.assertThat(listApplet.control("JComboBoxIdentityCardType").isExist())
                .as("Поле Тип документа не найдено").isTrue();
        softly.assertThat(listApplet.control("SeriesNumID").isExist())
                .as("Поле Серия/номер документа не найдено").isTrue();

        //Проверка поиска клиента по адресу
        softly.assertThat(listApplet.control("JComboBoxAddressType").isExist())
                .as("Поле Тип адреса не найдено").isTrue();
        softly.assertThat(listApplet.control("PostalCode").isExist())
                .as("Поле Почтовый индекс не найдено").isTrue();
        softly.assertThat(listApplet.control("JComboBoxCountry").isExist())
                .as("Поле Страна не найдено").isTrue();
        softly.assertThat(listApplet.control("City").isExist())
                .as("Поле Город не найдено").isTrue();
        softly.assertThat(listApplet.control("UnnormalizedAddress").isExist())
                .as("Поле Ненормализованный адрес не найдено").isTrue();
        softly.assertThat(listApplet.control("Transfer").isExist())
                .as("Кнопка Передать не найдена").isTrue();
        softly.assertThat(listApplet.control("Clear").isExist())
                .as("Кнопка Очистить не найдена").isTrue();

        //Поиск клиента
        listApplet.control("JComboBoxIdentityCardType").selectComboBoxValue("Паспорт гражданина РФ");
        listApplet.control("SeriesNumID").setValue("4513123456");
        listApplet.control("Transfer").click;

        //Проверка вкладки Анкетные данные
        browser.goToView("MDM Personal Data View");
        listApplet = browser.onListApplet("MDM Personal Data Applet");

        softly.assertThat(listApplet.control("UCI").isExist())
                .as("Поле УИК не найдено").isTrue();
        softly.assertThat(listApplet.control("Last Name").isExist())
                .as("Поле Фамилия не найдено").isTrue();
        softly.assertThat(listApplet.control("First Name").isExist())
                .as("Поле Имя не найдено").isTrue();
        softly.assertThat(listApplet.control("Middle Name").isExist())
                .as("Поле Отчество не найдено").isTrue();
        softly.assertThat(listApplet.control("Birth Date").isExist())
                .as("Поле Дата рождения не найдено").isTrue();
        softly.assertThat(listApplet.control("Birth Place").isExist())
                .as("Поле Место рождения найдено").isTrue();
        softly.assertThat(listApplet.control("Citizen Ship").isExist())
                .as("Поле Гражданство не найдено").isTrue();

        //Проверка соответствия значений тегов в xml сообщении типа CustomerAddRq и значений полей на бизнес-компоненте MDM Personal Data
        softly.assertThat(customerAddRqType.getCustomerInfo().getPersonName().getLastName())
                .as("Неверное значение поля Фамилия").isEqualTo(personalDataBC.getFieldValue("Last Name"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getPersonName().getFirstName())
                .as("Неверное значение поля Имя").isEqualTo(personalDataBC.getFieldValue("First Name"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getPersonName().getMiddleName())
                .as("Неверное значение поля Отчество").isEqualTo(personalDataBC.getFieldValue("Middle Name"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getBirthDate())
                .as("Неверное значение поля Дата рождения").isEqualTo(personalDataBC.getFieldValue("Birth Date"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getBirthPlace())
                .as("Неверное значение поля Место рождения").isEqualTo(personalDataBC.getFieldValue("Birth Place"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getCitizenShip())
                .as("Неверное значение поля Гражданство").isEqualTo(personalDataBC.getFieldValue("Citizen Ship"));

        //Проверка вкладки Документы
        browser.goToView("MDM Identity Card View");
        listApplet = browser.onListApplet("MDM Identity Card Applet");

        softly.assertThat(listApplet.control("IdType").isExist())
                .as("Поле Тип документа не найдено").isTrue();
        softly.assertThat(listApplet.control("SeriesNumID").isExist())
                .as("Поле Серия и номер документа не найдено").isTrue();
        softly.assertThat(listApplet.control("Issue Data").isExist())
                .as("Поле Дата выдачи документа не найдено").isTrue();
        softly.assertThat(listApplet.control("Issued By").isExist())
                .as("Поле Кем выдан документ не найдено").isTrue();
        softly.assertThat(listApplet.control("Code").isExist())
                .as("Поле Код подразделения, выдавшего ДУЛ не найдено").isTrue();
        softly.assertThat(listApplet.control("SNILS").isExist())
                .as("Поле СНИЛС не найдено").isTrue();

        //Проверка соответствия значений тегов в xml сообщении типа CustomerAddRq и значений полей на бизнес-компоненте MDM Identity Card
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(0).getIdType())
                .as("Неверное значение поля Тип документа").isEqualTo("Паспорт гражданина РФ");
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(0).getIdSeries() + " " +
                (customerAddRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(0).getIdNum()))
                .as("Неверное значение поля Серия и номер документа").isEqualTo("45 13 123456");
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
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(1).getIdType())
                .as("Неверное значение поля Тип документа").isEqualTo("Загранпаспорт гражданина РФ");
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(1).getIdSeries() + " " +
                (customerAddRqType.getCustomerInfo().getListOfIdentityCard().getIdentityCard().get(1).getIdNum()))
                .as("Неверное значение поля Серия и номер документа").isEqualTo("11 2494799");
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
        softly.assertThat(customerAddRqType.getCustomerInfo().getSnils())
                .as("Неверное значение поля СНИЛС").isEqualTo(customerIdentityCardBC.getFieldValue("SNILS"));

        //Проверка вкладки Адреса
        browser.goToView("MDM Customer Address View");
        listApplet = browser.onListApplet("MDM Customer Address Applet");

        softly.assertThat(listApplet.control("AddressType").isExist())
                .as("Поле Тип адреса не найдено").isTrue();
        softly.assertThat(listApplet.control("Country").isExist())
                .as("Поле Страна не найдено").isTrue();
        softly.assertThat(listApplet.control("PostalCode").isExist())
                .as("Поле Почтовый индекс не найдено").isTrue();
        softly.assertThat(listApplet.control("City").isExist())
                .as("Поле Город не найдено").isTrue();
        softly.assertThat(listApplet.control("Street").isExist())
                .as("Поле Улица не найдено").isTrue();
        softly.assertThat(listApplet.control("HouseNum").isExist())
                .as("Поле Дом не найдено").isTrue();
        softly.assertThat(listApplet.control("HouseExt").isExist())
                .as("Поле Корпус не найдено").isTrue();
        softly.assertThat(listApplet.control("Unit").isExist())
                .as("Поле Строение не найдено").isTrue();
        softly.assertThat(listApplet.control("UnitNum").isExist())
                .as("Поле Квартира/офис не найдено").isTrue();

        //Проверка соответствия значений тегов в xml сообщении типа CustomerAddRq и значений полей на бизнес-компоненте MDM Customer Address
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfAddress().getAddress().get(0).getAddressType())
                .as("Неверное значение поля Тип адреса").isEqualTo("Адрес регистрации");
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
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfAddress().getAddress().get(1).getAddressType())
                .as("Неверное значение поля Тип адреса").isEqualTo("Адрес проживания");
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

        //Проверка вкладки Контакты
        browser.goToView("MDM Contact Information View");
        listApplet = browser.onListApplet("MDM Contact Information Applet");

        softly.assertThat(listApplet.control("ContactType").isExist())
                .as("Поле Тип контактной информации не найдено").isTrue();
        softly.assertThat(listApplet.control("ContactNum").isExist())
                .as("Поле Значение не найдено").isTrue();

        //Проверка соответствия значений тегов в xml сообщении типа CustomerAddRq и значений полей на бизнес-компоненте MDM Contact Information
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfContactData().getContactData().get(0).getContactType())
                .as("Неверное значение поля Тип контактной информации").isEqualTo(contactInformationBC.getFieldValue("Cell Phone"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfContactData().getContactData().get(0).getContactNum())
                .as("Неверное значение поля Значение").isEqualTo("+79037680102");
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfContactData().getContactData().get(1).getContactType())
                .as("Неверное значение поля Тип контактной информации").isEqualTo(contactInformationBC.getFieldValue("Home Phone"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfContactData().getContactData().get(1).getContactNum())
                .as("Неверное значение поля Значение").isEqualTo("+74957540102");
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfContactData().getContactData().get(2).getContactType())
                .as("Неверное значение поля Тип контактной информации").isEqualTo(contactInformationBC.getFieldValue("Work Phone"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfContactData().getContactData().get(2).getContactNum())
                .as("Неверное значение поля Значение").isEqualTo("79037680103");
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfContactData().getContactData().get(3).getContactType())
                .as("Неверное значение поля Тип контактной информации").isEqualTo(contactInformationBC.getFieldValue("Email"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfContactData().getContactData().get(3).getContactNum())
                .as("Неверное значение поля Значение").isEqualTo("ivanov@mail.ru");

        //Проверка вкладки Место работы
        browser.goToView("MDM Employment History View");
        listApplet = browser.onListApplet("MDM Employment History Applet");

        softly.assertThat(listApplet.control("Employer").isExist())
                .as("Поле Работодатель не найдено").isTrue();
        softly.assertThat(listApplet.control("Position").isExist())
                .as("Поле Должность не найдено").isTrue();

        //Проверка соответствия значений тегов в xml сообщении типа CustomerAddRq и значений полей на бизнес-компоненте MDM Employment History
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfEmploymentHistory().getEmploymentHistory().get(0).getLegalName())
                .as("Неверное значение поля Работодатель").isEqualTo(employmentHistoryBC.getFieldValue("Employer"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfEmploymentHistory().getEmploymentHistory().get(0).getJob())
                .as("Неверное значение поля Должность").isEqualTo(employmentHistoryBC.getFieldValue("Position"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfEmploymentHistory().getEmploymentHistory().get(1).getLegalName())
                .as("Неверное значение поля Работодатель").isEqualTo(employmentHistoryBC.getFieldValue("Employer"));
        softly.assertThat(customerAddRqType.getCustomerInfo().getListOfEmploymentHistory().getEmploymentHistory().get(1).getJob())
                .as("Неверное значение поля Должность").isEqualTo(employmentHistoryBC.getFieldValue("Position"));

        //Проверка вкладки Активы
        browser.goToView("MDM Vehicle Information View");
        listApplet = browser.onListApplet("MDM Vehicle Information Applet");

        softly.assertThat(listApplet.control("Vehicle Brand").isExist())
                .as("Поле Марка ТС не найдено").isTrue();
        softly.assertThat(listApplet.control("Vehicle Num").isExist())
                .as("Поле Регистрационный номер ТС не найдено").isTrue();
        softly.assertThat(listApplet.control("Vehicle Age").isExist())
                .as("Поле Возраст ТС в годах не найдено").isTrue();
        softly.assertThat(listApplet.control("Purchase Year").isExist())
                .as("Поле Год приобретения не найдено").isTrue();
        softly.assertThat(listApplet.control("Market Price").isExist())
                .as("Поле Рыночная цена не найдено").isTrue();

        //Проверка соответствия значений тегов в xml сообщении типа CustomerAddRq и значений полей на бизнес-компоненте MDM Vehicle Information
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
        softly.assertAll();
    }
}