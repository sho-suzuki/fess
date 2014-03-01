/*
 * Copyright 2009-2014 the CodeLibs Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package jp.sf.fess.action.admin.dict;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import jp.sf.fess.crud.CommonConstants;
import jp.sf.fess.crud.CrudMessageException;
import jp.sf.fess.crud.util.SAStrutsUtil;
import jp.sf.fess.dict.DictionaryExpiredException;
import jp.sf.fess.dict.userdict.UserDictItem;
import jp.sf.fess.form.admin.dict.UserDictForm;
import jp.sf.fess.helper.SystemHelper;
import jp.sf.fess.pager.UserDictPager;
import jp.sf.fess.service.UserDictService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codelibs.core.util.StringUtil;
import org.codelibs.sastruts.core.annotation.Token;
import org.codelibs.sastruts.core.exception.SSCActionMessagesException;
import org.seasar.framework.beans.util.Beans;
import org.seasar.struts.annotation.ActionForm;
import org.seasar.struts.annotation.Execute;
import org.seasar.struts.exception.ActionMessagesException;

public class UserDictAction {

    private static final Log log = LogFactory.getLog(UserDictAction.class);

    @Resource
    @ActionForm
    protected UserDictForm userDictForm;

    @Resource
    protected UserDictService userDictService;

    @Resource
    protected UserDictPager userDictPager;

    @Resource
    protected SystemHelper systemHelper;

    public List<UserDictItem> userDictItemItems;

    public String getHelpLink() {
        return systemHelper.getHelpLink("dict");
    }

    protected String displayList(final boolean redirect) {
        // page navi
        userDictItemItems = userDictService.getUserDictList(
                userDictForm.dictId, userDictPager);

        // restore from pager
        Beans.copy(userDictPager, userDictForm.searchParams)
                .excludes(CommonConstants.PAGER_CONVERSION_RULE).execute();

        if (redirect) {
            return "index?dictId=" + userDictForm.dictId + "&redirect=true";
        } else {
            return "index.jsp";
        }
    }

    @Execute(validator = false, input = "error.jsp")
    public String index() {
        return displayList(false);
    }

    @Execute(validator = false, input = "error.jsp", urlPattern = "list/{dictId}/{pageNumber}")
    public String list() {
        // page navi
        if (StringUtil.isNotBlank(userDictForm.pageNumber)) {
            try {
                userDictPager.setCurrentPageNumber(Integer
                        .parseInt(userDictForm.pageNumber));
            } catch (final NumberFormatException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Invalid value: " + userDictForm.pageNumber, e);
                }
            }
        }

        return displayList(false);
    }

    @Execute(validator = false, input = "error.jsp")
    public String search() {
        Beans.copy(userDictForm.searchParams, userDictPager)
                .excludes(CommonConstants.PAGER_CONVERSION_RULE).execute();

        return displayList(false);
    }

    @Execute(validator = false, input = "error.jsp")
    public String reset() {
        userDictPager.clear();

        return displayList(false);
    }

    @Execute(validator = false, input = "error.jsp")
    public String back() {
        return displayList(false);
    }

    @Token(save = true, validate = false)
    @Execute(validator = false, input = "error.jsp")
    public String editagain() {
        return "edit.jsp";
    }

    @Execute(validator = false, input = "error.jsp", urlPattern = "confirmpage/{dictId}/{crudMode}/{id}")
    public String confirmpage() {
        if (userDictForm.crudMode != CommonConstants.CONFIRM_MODE) {
            throw new ActionMessagesException("errors.crud_invalid_mode",
                    new Object[] { CommonConstants.CONFIRM_MODE,
                            userDictForm.crudMode });
        }

        loadUserDict();

        return "confirm.jsp";
    }

    @Token(save = true, validate = false)
    @Execute(validator = false, input = "error.jsp")
    public String createpage() {
        // page navi
        userDictForm.initialize();
        userDictForm.crudMode = CommonConstants.CREATE_MODE;

        return "edit.jsp";
    }

    @Token(save = true, validate = false)
    @Execute(validator = false, input = "error.jsp", urlPattern = "editpage/{dictId}/{crudMode}/{id}")
    public String editpage() {
        if (userDictForm.crudMode != CommonConstants.EDIT_MODE) {
            throw new ActionMessagesException("errors.crud_invalid_mode",
                    new Object[] { CommonConstants.EDIT_MODE,
                            userDictForm.crudMode });
        }

        loadUserDict();

        return "edit.jsp";
    }

    @Token(save = true, validate = false)
    @Execute(validator = false, input = "error.jsp")
    public String editfromconfirm() {
        userDictForm.crudMode = CommonConstants.EDIT_MODE;

        loadUserDict();

        return "edit.jsp";
    }

    @Token(save = false, validate = true, keep = true)
    @Execute(validator = true, input = "edit.jsp")
    public String confirmfromcreate() {
        return "confirm.jsp";
    }

    @Token(save = false, validate = true, keep = true)
    @Execute(validator = true, input = "edit.jsp")
    public String confirmfromupdate() {
        return "confirm.jsp";
    }

    @Token(save = true, validate = false)
    @Execute(validator = false, input = "error.jsp", urlPattern = "deletepage/{dictId}/{crudMode}/{id}")
    public String deletepage() {
        if (userDictForm.crudMode != CommonConstants.DELETE_MODE) {
            throw new ActionMessagesException("errors.crud_invalid_mode",
                    new Object[] { CommonConstants.DELETE_MODE,
                            userDictForm.crudMode });
        }

        loadUserDict();

        return "confirm.jsp";
    }

    @Token(save = true, validate = false)
    @Execute(validator = false, input = "error.jsp")
    public String deletefromconfirm() {
        userDictForm.crudMode = CommonConstants.DELETE_MODE;

        loadUserDict();

        return "confirm.jsp";
    }

    @Token(save = false, validate = true)
    @Execute(validator = true, input = "edit.jsp")
    public String create() {
        try {
            final UserDictItem userDictItem = createUserDict();
            userDictService.store(userDictForm.dictId, userDictItem);
            SAStrutsUtil.addSessionMessage("success.crud_create_crud_table");

            return displayList(true);
        } catch (final DictionaryExpiredException e) {
            throw e;
        } catch (final ActionMessagesException e) {
            log.error(e.getMessage(), e);
            throw e;
        } catch (final CrudMessageException e) {
            log.error(e.getMessage(), e);
            throw new SSCActionMessagesException(e, e.getMessageId(),
                    e.getArgs());
        } catch (final Exception e) {
            log.error(e.getMessage(), e);
            throw new SSCActionMessagesException(e,
                    "errors.crud_failed_to_create_crud_table");
        }
    }

    @Token(save = false, validate = true)
    @Execute(validator = true, input = "edit.jsp")
    public String update() {
        try {
            final UserDictItem userDictItem = createUserDict();
            userDictService.store(userDictForm.dictId, userDictItem);
            SAStrutsUtil.addSessionMessage("success.crud_update_crud_table");

            return displayList(true);
        } catch (final DictionaryExpiredException e) {
            throw e;
        } catch (final ActionMessagesException e) {
            log.error(e.getMessage(), e);
            throw e;
        } catch (final CrudMessageException e) {
            log.error(e.getMessage(), e);
            throw new SSCActionMessagesException(e, e.getMessageId(),
                    e.getArgs());
        } catch (final Exception e) {
            log.error(e.getMessage(), e);
            throw new SSCActionMessagesException(e,
                    "errors.crud_failed_to_update_crud_table");
        }
    }

    @Token(save = false, validate = true)
    @Execute(validator = false, input = "error.jsp")
    public String delete() {
        if (userDictForm.crudMode != CommonConstants.DELETE_MODE) {
            throw new ActionMessagesException("errors.crud_invalid_mode",
                    new Object[] { CommonConstants.DELETE_MODE,
                            userDictForm.crudMode });
        }

        try {
            final UserDictItem userDictItem = userDictService.getUserDict(
                    userDictForm.dictId, createKeyMap());
            if (userDictItem == null) {
                // throw an exception
                throw new ActionMessagesException(
                        "errors.crud_could_not_find_crud_table",

                        new Object[] { userDictForm.id });

            }

            userDictService.delete(userDictForm.dictId, userDictItem);
            SAStrutsUtil.addSessionMessage("success.crud_delete_crud_table");

            return displayList(true);
        } catch (final DictionaryExpiredException e) {
            throw e;
        } catch (final ActionMessagesException e) {
            log.error(e.getMessage(), e);
            throw e;
        } catch (final CrudMessageException e) {
            log.error(e.getMessage(), e);
            throw new SSCActionMessagesException(e, e.getMessageId(),
                    e.getArgs());
        } catch (final Exception e) {
            log.error(e.getMessage(), e);
            throw new SSCActionMessagesException(e,
                    "errors.crud_failed_to_delete_crud_table");
        }
    }

    protected void loadUserDict() {

        final UserDictItem userDictItem = userDictService.getUserDict(
                userDictForm.dictId, createKeyMap());
        if (userDictItem == null) {
            // throw an exception
            throw new ActionMessagesException(
                    "errors.crud_could_not_find_crud_table",
                    new Object[] { userDictForm.id });

        }

        userDictForm.id = Long.toString(userDictItem.getId());
        userDictForm.token = userDictItem.getToken();
        userDictForm.segmentation = userDictItem.getSegmentation();
        userDictForm.reading = userDictItem.getReading();
        userDictForm.pos = userDictItem.getPos();
    }

    protected UserDictItem createUserDict() {
        UserDictItem userDictItem;
        if (userDictForm.crudMode == CommonConstants.EDIT_MODE) {
            userDictItem = userDictService.getUserDict(userDictForm.dictId,
                    createKeyMap());
            if (userDictItem == null) {
                // throw an exception
                throw new ActionMessagesException(
                        "errors.crud_could_not_find_crud_table",

                        new Object[] { userDictForm.id });

            }
        } else {
            userDictItem = new UserDictItem(0, StringUtil.EMPTY,
                    StringUtil.EMPTY, StringUtil.EMPTY, StringUtil.EMPTY);
        }

        userDictItem.setNewToken(userDictForm.token);
        userDictItem.setNewSegmentation(userDictForm.segmentation);
        userDictItem.setNewReading(userDictForm.reading);
        userDictItem.setNewPos(userDictForm.pos);

        return userDictItem;
    }

    protected Map<String, String> createKeyMap() {
        final Map<String, String> keys = new HashMap<String, String>();
        keys.put("id", userDictForm.id);
        return keys;
    }

}
