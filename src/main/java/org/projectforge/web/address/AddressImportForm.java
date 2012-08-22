/////////////////////////////////////////////////////////////////////////////
//
// Project   ProjectForge
//
// Copyright 2001-2009, Micromata GmbH, Kai Reinhard
//           All rights reserved.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.web.address;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.vcard.Parameter;
import net.fortuna.ical4j.vcard.Parameter.Id;
import net.fortuna.ical4j.vcard.Property;
import net.fortuna.ical4j.vcard.VCard;
import net.fortuna.ical4j.vcard.VCardBuilder;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.joda.time.DateTime;
import org.projectforge.address.AddressDO;
import org.projectforge.address.AddressDao;
import org.projectforge.address.AddressStatus;
import org.projectforge.address.ContactStatus;
import org.projectforge.address.FormOfAddress;
import org.projectforge.address.PersonalAddressDao;
import org.projectforge.core.BaseSearchFilter;
import org.projectforge.core.QueryFilter;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.ListPage;
import org.projectforge.web.wicket.flowlayout.DivType;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.FileUploadPanel;


/**
 * @author Maximilian Lauterbach (m.lauterbach@micromata.de)
 * 
 */
@ListPage(editPage = AddressEditPage.class)
public class AddressImportForm extends AbstractEditForm<AddressDO, AddressImportPage>
{
  private static final long serialVersionUID = -1691614676645602272L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AddressImportForm.class);

  @SpringBean(name = "personalAddressDao")
  private PersonalAddressDao personalAddressDao;

  private final List<FileUpload> uploads = new LinkedList<FileUpload>();

  /**
   * @param parentPage
   * @param data
   */
  public AddressImportForm(final AddressImportPage parentPage, final AddressDO data)
  {
    super(parentPage, data);
  }

  /**
   * @see org.apache.wicket.Component#onInitialize()
   */
  @Override
  protected void onInitialize()
  {
    super.onInitialize();
    gridBuilder.newGrid16().newColumnsPanel().newColumnPanel(DivType.COL_50);
    final FieldsetPanel newFieldset = gridBuilder.newFieldset("File upload");
    newFieldset.add(new FileUploadPanel(newFieldset.newChildId(), new FileUploadField(FileUploadPanel.WICKET_ID,
        new PropertyModel<List<FileUpload>>(this, "uploads"))));
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditForm#getLogger()
   */
  @Override
  protected Logger getLogger()
  {
    return log;
  }

  public void create()
  {
    if(uploads.size() > 0) {
      final FileUpload upload = uploads.get(0);
      try {
        final File file = upload.writeToTempFile();

        final FileInputStream fis = new FileInputStream(file);
        final VCardBuilder builder = new VCardBuilder(fis);
        final VCard card = builder.build();

        ////// SET BASE DATA
        setName(card.getProperty(net.fortuna.ical4j.vcard.Property.Id.N));
        setOrganization(card.getProperty(net.fortuna.ical4j.vcard.Property.Id.ORG));
        setBirth(card.getProperty(net.fortuna.ical4j.vcard.Property.Id.BDAY));
        setNote(card.getProperty(net.fortuna.ical4j.vcard.Property.Id.NOTE));

        ////// SET ADDITIONAL DATA
        final List<Property> li = card.getProperties();
        setProperties(li);

        // handle item entries
        final VCardItemElementHandler ih = new VCardItemElementHandler(new FileInputStream(file));
        if (!ih.getItemList().isEmpty())
          setProperties(ih.getItemList());

        data.setAddressStatus(AddressStatus.UPTODATE);
        data.setDeleted(false);
        data.setLastUpdate(DateTime.now().toDate());
        data.setCreated(DateTime.now().toDate());
        data.setContactStatus(ContactStatus.ACTIVE);
        data.setForm(FormOfAddress.UNKNOWN);

        ///// CHECK FOR DOUBLE ENTRIES
        final BaseSearchFilter af = new BaseSearchFilter();
        af.setSearchString(data.getName() + " " + data.getFirstName());
        final AddressDao dao = (AddressDao) getBaseDao();
        final QueryFilter queryFilter = new QueryFilter(af);
        final List<AddressDO> list = dao.internalGetList(queryFilter);

        ////// SAVING
        if (list.size() == 0){
          getBaseDao().save(data);
          final PageParameters params = new PageParameters();
          params.add(AbstractEditPage.PARAMETER_KEY_ID, data.getId());
          final AddressEditPage addressEditPage = new AddressEditPage(params);
          addressEditPage.newEditForm(parentPage, data);
          setResponsePage(addressEditPage);
        } else {
          //          AddressListPage alp = new AddressListPage(parameters)
          //          setResponsePage(new addressli)
          System.out.println("EINTRAG SCHON VORHANDEN");
        }


      } catch (final IOException ex) {
        log.fatal("Exception encountered " + ex, ex);
      } catch (final ParserException ex) {
        log.fatal("Exception encountered " + ex, ex);
      }
    }
  }

  /**
   * @param property
   */
  private void setNote(final Property property)
  {
    data.setComment(property.getValue());
  }

  /**
   * @param li
   */
  private void setProperties(final List<Property> li)
  {
    for (final Property property : li){
      final List<Parameter> lii = property.getParameters(Id.TYPE);
      for (final Parameter param : lii){
        if (param.getValue().equals("HOME"))
          setHomeData(property);
        else if (param.getValue().equals("WORK"))
          setWorkData(property);
        else if (param.getValue().equals("OTHER"))
          setOtherData(property);
      }
    }
  }

  /**
   * @param property
   */
  private void setBirth(final Property property)
  {
    // TODO date fix
    //    System.out.println(DateTime.parse(property.getValue()));
    //    data.setBirthday(new Date(DateTime.parse(property.getValue()).getMillis()));
    //    System.out.println(data.getBirthday());
  }

  private void setName(final Property property){
    final String str[] = StringUtils.split(property.getValue(), ';');
    data.setName(str[0]);
    data.setFirstName(str[1]);
    if (str.length >= 3)
      data.setTitle(str[2]);
  }

  private void setOrganization(final Property property){
    final String org = StringUtils.substringBefore(property.getValue(), ";");
    data.setOrganization(org);
    final String division = StringUtils.substringAfter(property.getValue(), ";");
    data.setDivision(division);
  }

  /**
   * Create home data
   * 
   * @param property
   */
  private void setHomeData(final Property property){
    boolean telCheck = true; // to seperate phone and mobil number
    ////// SET HOME EMAIL
    if (property.getId().toString().equals("EMAIL"))
      data.setPrivateEmail(property.getValue());

    ////// SET HOME PHONE
    if (property.getId().toString().equals("TEL")){
      final List<Parameter> list = property.getParameters();
      for (final Parameter p : list){
        if (p.getValue().toString().equals("VOICE")){
          final String tel = getTel(property.getValue());

          // phone number first, mobil number second
          if (telCheck) {
            data.setPrivatePhone(tel);
            telCheck = false;
          } else
            data.setPrivateMobilePhone(tel);
        }
      }
    }

    ////// SET FAX -> no private fax

    ////// SET HOME ADDRESS
    if (property.getId().toString().equals("ADR")){
      final String str[] = StringUtils.split(property.getValue(), ';');
      final int size = str.length;
      if (size >= 1)
        data.setPrivateAddressText(str[0]);
      if (size >= 2)
        data.setPrivateCity(str[1]);
      if (size >= 3)
        data.setPrivateZipCode(str[2]);
      if (size >= 4)
        data.setPrivateCountry(str[3]);
      if (size >= 5)
        data.setPrivateState(str[4]);
    }

    ////// SET HOME URL
    //    if (property.getId().toString().equals("URL"))
    //      data.setWebsite(property.getValue());
  }

  /**
   * @param property
   * @return
   */
  private String getTel(String tel)
  {
    if (tel.startsWith("0")){
      tel = "+49 " + tel.substring(1);
    } else {
      if (!tel.startsWith("+"))
        tel = "+49 " + tel;
    }
    return tel;
  }

  private void setWorkData(final Property property){
    boolean telCheck = true; // to seperate phone and mobil number

    ////// SET WORK PHONE
    if (property.getId().toString().equals("TEL")){
      final List<Parameter> list = property.getParameters();
      for (final Parameter p : list){
        if (p.getValue().toString().equals("VOICE")){
          final String tel = getTel(property.getValue());

          // phone number first, mobil number second
          if (telCheck) {
            data.setBusinessPhone(tel);
            telCheck = false;
          } else
            data.setMobilePhone(tel);
        }
        ////// SET WORK FAX
        if (p.getValue().toString().equals("FAX")){
          data.setFax(getTel(property.getValue()));
        }
      }
    }

    ////// SET WORK EMAIL
    if (property.getId().toString().equals("EMAIL"))
      data.setEmail(property.getValue());


    ////// SET WORK ADDRESS
    if (property.getId().toString().equals("ADR")){
      final String str[] = StringUtils.split(property.getValue(), ';');
      final int size = str.length;
      if (size >= 1)
        data.setAddressText(str[0]);
      if (size >= 2)
        data.setCity(str[1]);
      if (size >= 3)
        data.setZipCode(str[2]);
      if (size >= 4)
        data.setCountry(str[3]);
      if (size >= 5)
        data.setState(str[4]);
    }

    ////// SET WORK URL
    if (property.getId().toString().equals("URL"))
      data.setWebsite(property.getValue());
  }

  private void setOtherData(final Property property){
    //    System.out.println("OTHER " + property.getId() + " " + property.getValue());
  }
}
