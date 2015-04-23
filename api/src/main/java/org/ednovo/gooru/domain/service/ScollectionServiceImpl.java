/////////////////////////////////////////////////////////////
// ScollectionServiceImpl.java
// gooru-api
// Created by Gooru on 2014
// Copyright (c) 2014 Gooru. All rights reserved.
// http://www.goorulearning.org/
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
/////////////////////////////////////////////////////////////
package org.ednovo.gooru.domain.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.commons.lang.ArrayUtils;
import org.ednovo.gooru.application.util.AsyncExecutor;
import org.ednovo.gooru.application.util.CollectionUtil;
import org.ednovo.gooru.application.util.MailAsyncExecutor;
import org.ednovo.gooru.application.util.ResourceImageUtil;
import org.ednovo.gooru.application.util.SerializerUtil;
import org.ednovo.gooru.application.util.TaxonomyUtil;
import org.ednovo.gooru.core.api.model.ActionResponseDTO;
import org.ednovo.gooru.core.api.model.AssessmentQuestion;
import org.ednovo.gooru.core.api.model.Code;
import org.ednovo.gooru.core.api.model.Collection;
import org.ednovo.gooru.core.api.model.CollectionItem;
import org.ednovo.gooru.core.api.model.CollectionType;
import org.ednovo.gooru.core.api.model.Content;
import org.ednovo.gooru.core.api.model.ContentAssociation;
import org.ednovo.gooru.core.api.model.ContentMetaAssociation;
import org.ednovo.gooru.core.api.model.ContentMetaDTO;
import org.ednovo.gooru.core.api.model.ContentSettings;
import org.ednovo.gooru.core.api.model.ContentType;
import org.ednovo.gooru.core.api.model.CustomTableValue;
import org.ednovo.gooru.core.api.model.Identity;
import org.ednovo.gooru.core.api.model.License;
import org.ednovo.gooru.core.api.model.PartyCustomField;
import org.ednovo.gooru.core.api.model.Resource;
import org.ednovo.gooru.core.api.model.ResourceSource;
import org.ednovo.gooru.core.api.model.ResourceSummary;
import org.ednovo.gooru.core.api.model.ResourceType;
import org.ednovo.gooru.core.api.model.Sharing;
import org.ednovo.gooru.core.api.model.ShelfType;
import org.ednovo.gooru.core.api.model.StandardFo;
import org.ednovo.gooru.core.api.model.Textbook;
import org.ednovo.gooru.core.api.model.User;
import org.ednovo.gooru.core.api.model.UserCollectionItemAssoc;
import org.ednovo.gooru.core.api.model.UserGroupSupport;
import org.ednovo.gooru.core.api.model.UserSummary;
import org.ednovo.gooru.core.application.util.BaseUtil;
import org.ednovo.gooru.core.application.util.CustomProperties;
import org.ednovo.gooru.core.application.util.ResourceMetaInfo;
import org.ednovo.gooru.core.application.util.ServerValidationUtils;
import org.ednovo.gooru.core.cassandra.model.ResourceMetadataCo;
import org.ednovo.gooru.core.constant.ConstantProperties;
import org.ednovo.gooru.core.constant.Constants;
import org.ednovo.gooru.core.constant.ParameterProperties;
import org.ednovo.gooru.core.exception.BadRequestException;
import org.ednovo.gooru.core.exception.NotFoundException;
import org.ednovo.gooru.core.exception.UnauthorizedException;
import org.ednovo.gooru.domain.cassandra.service.ResourceCassandraService;
import org.ednovo.gooru.domain.service.assessment.AssessmentService;
import org.ednovo.gooru.domain.service.eventlogs.ClasspageEventLog;
import org.ednovo.gooru.domain.service.eventlogs.CollectionEventLog;
import org.ednovo.gooru.domain.service.partner.CustomFieldsService;
import org.ednovo.gooru.domain.service.redis.RedisService;
import org.ednovo.gooru.domain.service.resource.ResourceManager;
import org.ednovo.gooru.domain.service.resource.ResourceService;
import org.ednovo.gooru.domain.service.taxonomy.TaxonomyService;
import org.ednovo.gooru.domain.service.user.UserService;
import org.ednovo.gooru.domain.service.v2.ContentService;
import org.ednovo.gooru.infrastructure.mail.MailHandler;
import org.ednovo.gooru.infrastructure.messenger.IndexHandler;
import org.ednovo.gooru.infrastructure.messenger.IndexProcessor;
import org.ednovo.gooru.infrastructure.persistence.hibernate.CollectionRepository;
import org.ednovo.gooru.infrastructure.persistence.hibernate.UserRepository;
import org.ednovo.gooru.infrastructure.persistence.hibernate.collaborator.CollaboratorRepository;
import org.ednovo.gooru.infrastructure.persistence.hibernate.content.ContentAssociationRepository;
import org.ednovo.gooru.infrastructure.persistence.hibernate.content.ContentRepositoryHibernate;
import org.ednovo.gooru.infrastructure.persistence.hibernate.customTable.CustomTableRepository;
import org.ednovo.gooru.infrastructure.persistence.hibernate.question.CommentRepository;
import org.ednovo.gooru.infrastructure.persistence.hibernate.resource.ResourceRepository;
import org.ednovo.gooru.infrastructure.persistence.hibernate.taxonomy.TaxonomyRespository;
import org.ednovo.gooru.json.serializer.util.JsonSerializer;
import org.ednovo.gooru.security.OperationAuthorizer;
import org.ednovo.goorucore.application.serializer.JsonDeserializer;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

import flexjson.JSONSerializer;

public class ScollectionServiceImpl extends BaseServiceImpl implements ScollectionService, ParameterProperties, ConstantProperties {

	@Autowired
	private CollectionRepository collectionRepository;

	@Autowired
	private CollectionEventLog collectionEventLog;

	@Autowired
	private ResourceRepository resourceRepository;

	@Autowired
	private CollectionUtil collectionUtil;

	@Autowired
	private MailHandler mailHandler;

	@Autowired
	private TaxonomyRespository taxonomyRepository;

	@Autowired
	private UserService userService;

	@Autowired
	protected AssessmentService assessmentService;

	@Autowired
	private ContentRepositoryHibernate contentRepositoryHibernate;

	@Autowired
	private ContentAssociationRepository contentAssociationRepository;

	@Autowired
	protected TaxonomyService taxonomyService;

	@Autowired
	private ResourceService resourceService;

	@Autowired
	private ResourceImageUtil resourceImageUtil;

	@Autowired
	private ContentService contentService;

	@Autowired
	private ResourceCassandraService resourceCassandraService;

	@Autowired
	private FeedbackService feedbackService;

	@Autowired
	@javax.annotation.Resource(name = "resourceManager")
	private ResourceManager resourceManager;

	@Autowired
	private CustomTableRepository customTableRepository;

	@Autowired
	private MailAsyncExecutor mailAsyncExecutor;

	@Autowired
	private AsyncExecutor asyncExecutor;

	@Autowired
	private CommentRepository commentRepository;

	@Autowired
	private CollaboratorRepository collaboratorRepository;

	@Autowired
	private RedisService redisService;

	@Autowired
	private CustomFieldsService customFieldsService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private OperationAuthorizer operationAuthorizer;

	@Autowired
	private PartyService partyService;
	
	@Autowired
	private IndexHandler indexHandler;
	
	@Autowired
	private ClasspageEventLog classpageEventLog;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ScollectionServiceImpl.class);

	@Override
	public ActionResponseDTO<Collection> createCollection(final Collection collection, final boolean addToShelf, final String resourceId, final String taxonomyCode, final boolean updateTaxonomyByCode, final String parentId) throws Exception {
		
		final Errors errors = validateCollection(collection);
		if (!errors.hasErrors()) {
			if (taxonomyCode != null) {
				addCollectionTaxonomy(collection, taxonomyCode, updateTaxonomyByCode);
			}
			collection.setBuildType(this.getCustomTableRepository().getCustomTableValue(CustomProperties.Table.BUILD_TYPE.getTable(), collection.getBuildType() == null ? WEB : collection.getBuildType().getValue() != null ? collection.getBuildType().getValue() : WEB));

			if (collection.getCollectionType() != null && collection.getCollectionType().equalsIgnoreCase(ResourceType.Type.FOLDER.getType())) {
				collection.setResourceFormat(this.getCustomTableRepository().getCustomTableValue(RESOURCE_CATEGORY_FORMAT, FOLDER));
			} else if (collection.getCollectionType() != null && collection.getCollectionType().equalsIgnoreCase(ResourceType.Type.ASSESSMENT.getType())) {
				collection.setResourceFormat(this.getCustomTableRepository().getCustomTableValue(RESOURCE_CATEGORY_FORMAT, ASSESSMENT));
			} else {
				collection.setResourceFormat(this.getCustomTableRepository().getCustomTableValue(RESOURCE_CATEGORY_FORMAT, SCOLLECTION));
			}

			this.getCollectionRepository().save(collection);
			if (resourceId != null && !resourceId.isEmpty()) {
				CollectionItem collectionItem = new CollectionItem();
				collectionItem.setItemType(ShelfType.AddedType.ADDED.getAddedType());
				collectionItem = this.createCollectionItem(resourceId, collection.getGooruOid(), collectionItem, collection.getUser(), CollectionType.COLLECTION.getCollectionType(), false).getModel();
				final Set<CollectionItem> collectionItems = new TreeSet<CollectionItem>();
				collectionItems.add(collectionItem);
				collection.setCollectionItems(collectionItems);
			}

			if (addToShelf) {
				final CollectionItem collectionItem = new CollectionItem();
				collectionItem.setItemType(ShelfType.AddedType.ADDED.getAddedType());
				collection.setCollectionItem(this.createCollectionItem(collection.getGooruOid(), null, collectionItem, collection.getUser(), CollectionType.SHElf.getCollectionType(), false).getModel());
			}
			if (collection.getResourceType().getName().equalsIgnoreCase(SCOLLECTION)) {
				if (collection.getDepthOfKnowledges() != null && collection.getDepthOfKnowledges().size() > 0) {
					collection.setDepthOfKnowledges(this.updateContentMeta(collection.getDepthOfKnowledges(), collection, collection.getUser(), DEPTH_OF_KNOWLEDGE));
				} else {
					collection.setDepthOfKnowledges(this.setContentMetaAssociation(this.getContentMetaAssociation(DEPTH_OF_KNOWLEDGE), collection, DEPTH_OF_KNOWLEDGE));
				}

				if (collection.getLearningSkills() != null && collection.getLearningSkills().size() > 0) {
					collection.setLearningSkills(this.updateContentMeta(collection.getLearningSkills(), collection, collection.getUser(), LEARNING_AND_INNOVATION_SKILLS));
				} else {
					collection.setLearningSkills(this.setContentMetaAssociation(this.getContentMetaAssociation(LEARNING_AND_INNOVATION_SKILLS), collection, LEARNING_AND_INNOVATION_SKILLS));
				}

				if (collection.getAudience() != null && collection.getAudience().size() > 0) {
					collection.setAudience(this.updateContentMeta(collection.getAudience(), collection, collection.getUser(), AUDIENCE));
				} else {
					collection.setAudience(this.setContentMetaAssociation(this.getContentMetaAssociation(AUDIENCE), collection, AUDIENCE));
				}

				if (collection.getInstructionalMethod() != null && collection.getInstructionalMethod().size() > 0) {
					collection.setInstructionalMethod(this.updateContentMeta(collection.getInstructionalMethod(), collection, collection.getUser(), INSTRUCTIONAL_METHOD));
				} else {
					collection.setInstructionalMethod(this.setContentMetaAssociation(this.getContentMetaAssociation(INSTRUCTIONAL_METHOD), collection, INSTRUCTIONAL_METHOD));
				}
			}

			final Collection parentCollection = collectionRepository.getCollectionByGooruOid(parentId, collection.getUser().getGooruUId());
			if (parentCollection != null) {
				collection.setCollectionItem(this.createCollectionItem(collection.getGooruOid(), parentCollection.getGooruOid(), new CollectionItem(), collection.getUser(), CollectionType.FOLDER.getCollectionType(), false).getModel());
			}
			collection.setClusterUid(collection.getGooruOid());
			collection.setIsRepresentative(1);
			this.getCollectionRepository().save(collection);

			try {
				indexHandler.setReIndexRequest(collection.getGooruOid(), IndexProcessor.INDEX, SCOLLECTION, null, false, false);						
			} catch (Exception e) {
				LOGGER.error(_ERROR, e);
			}
			getAsyncExecutor().deleteFromCache(V2_ORGANIZE_DATA + collection.getUser().getPartyUid() + "*");

		}

		this.getCollectionEventLog().getEventLogs(collection.getCollectionItem(), true, false, collection.getUser(), false, false, null);
        return new ActionResponseDTO<Collection>(collection, errors);
	}

	@Override
	public ActionResponseDTO<Collection> createCollection(final Collection collection, final boolean addToShelf, final String resourceId, final String parentId, final User user) throws Exception {
		final Errors errors = validateCollection(collection);
		if (!errors.hasErrors()) {
			Collection parentCollection =  null;
			if (parentId != null) { 				
				parentCollection = collectionRepository.getCollectionByGooruOid(parentId, collection.getUser().getGooruUId());
			}
			collection.setBuildType(this.getCustomTableRepository().getCustomTableValue(CustomProperties.Table.BUILD_TYPE.getTable(), collection.getBuildType() == null ? WEB : collection.getBuildType().getValue() != null ? collection.getBuildType().getValue() : WEB));
			if (collection.getCollectionType() != null && collection.getCollectionType().equalsIgnoreCase(ResourceType.Type.FOLDER.getType())) {
				collection.setResourceFormat(this.getCustomTableRepository().getCustomTableValue(RESOURCE_CATEGORY_FORMAT, FOLDER));
			} else if (collection.getCollectionType() != null && collection.getCollectionType().equalsIgnoreCase(ResourceType.Type.ASSESSMENT.getType())) {
				collection.setResourceFormat(this.getCustomTableRepository().getCustomTableValue(RESOURCE_CATEGORY_FORMAT, ASSESSMENT));
			} else {
				collection.setResourceFormat(this.getCustomTableRepository().getCustomTableValue(RESOURCE_CATEGORY_FORMAT, SCOLLECTION));
			}
			if (collection.getSharing() != null && !collection.getCollectionType().equalsIgnoreCase(ResourceType.Type.ASSESSMENT_URL.getType()) &&  collection.getSharing().equalsIgnoreCase(PUBLIC)) {
				collection.setPublishStatus(this.getCustomTableRepository().getCustomTableValue(_PUBLISH_STATUS, PENDING));
				collection.setSharing(Sharing.ANYONEWITHLINK.getSharing());
			}
			this.getCollectionRepository().save(collection);
			this.getResourceService().saveOrUpdateResourceTaxonomy(collection, collection.getTaxonomySet());
			if (resourceId != null && !resourceId.isEmpty()) {
				CollectionItem collectionItem = new CollectionItem();
				collectionItem.setItemType(ShelfType.AddedType.ADDED.getAddedType());
				collectionItem = this.createCollectionItem(resourceId, collection.getGooruOid(), collectionItem, collection.getUser(), CollectionType.COLLECTION.getCollectionType(), false).getModel();
				final Set<CollectionItem> collectionItems = new TreeSet<CollectionItem>();
				collectionItems.add(collectionItem);
				collection.setCollectionItems(collectionItems);
			}
			
			if (addToShelf) {
				final CollectionItem collectionItem = new CollectionItem();
				collectionItem.setItemType(ShelfType.AddedType.ADDED.getAddedType());
				collection.setCollectionItem(this.createCollectionItem(collection.getGooruOid(), null, collectionItem, collection.getUser(), CollectionType.SHElf.getCollectionType(), false).getModel());
			}
			if (collection.getResourceType().getName().equalsIgnoreCase(SCOLLECTION)) {
				if (collection.getDepthOfKnowledges() != null && collection.getDepthOfKnowledges().size() > 0) {
					collection.setDepthOfKnowledges(this.updateContentMeta(collection.getDepthOfKnowledges(), collection, collection.getUser(), DEPTH_OF_KNOWLEDGE));
				} else {
					collection.setDepthOfKnowledges(this.setContentMetaAssociation(this.getContentMetaAssociation(DEPTH_OF_KNOWLEDGE), collection, DEPTH_OF_KNOWLEDGE));
				}

				if (collection.getLearningSkills() != null && collection.getLearningSkills().size() > 0) {
					collection.setLearningSkills(this.updateContentMeta(collection.getLearningSkills(), collection, collection.getUser(), LEARNING_AND_INNOVATION_SKILLS));
				} else {
					collection.setLearningSkills(this.setContentMetaAssociation(this.getContentMetaAssociation(LEARNING_AND_INNOVATION_SKILLS), collection, LEARNING_AND_INNOVATION_SKILLS));
				}

				if (collection.getAudience() != null && collection.getAudience().size() > 0) {
					collection.setAudience(this.updateContentMeta(collection.getAudience(), collection, collection.getUser(), AUDIENCE));
				} else {
					collection.setAudience(this.setContentMetaAssociation(this.getContentMetaAssociation(AUDIENCE), collection, AUDIENCE));
				}

				if (collection.getInstructionalMethod() != null && collection.getInstructionalMethod().size() > 0) {
					collection.setInstructionalMethod(this.updateContentMeta(collection.getInstructionalMethod(), collection, collection.getUser(), INSTRUCTIONAL_METHOD));
				} else {
					collection.setInstructionalMethod(this.setContentMetaAssociation(this.getContentMetaAssociation(INSTRUCTIONAL_METHOD), collection, INSTRUCTIONAL_METHOD));
				}
			}
			if (collection.getSharing().equalsIgnoreCase(PUBLIC)) {
				final UserSummary userSummary = this.getUserRepository().getSummaryByUid(user.getPartyUid());
				if (userSummary.getGooruUid() == null) {
					userSummary.setGooruUid(user.getPartyUid());
				}
				userSummary.setCollections((userSummary.getCollections() != null ? userSummary.getCollections() : 0) + 1);
				this.getUserRepository().save(userSummary);
			}

			if (parentCollection != null) {
				if (!collection.getCollectionType().equalsIgnoreCase(PRIVATE)) { 
					parentCollection.setSharing(collection.getSharing());
					this.getCollectionRepository().save(parentCollection);
				}
				collection.setCollectionItem(this.createCollectionItem(collection.getGooruOid(), parentCollection.getGooruOid(), new CollectionItem(), collection.getUser(), CollectionType.FOLDER.getCollectionType(), false).getModel());
				getAsyncExecutor().deleteFromCache(V2_ORGANIZE_DATA + parentCollection.getUser().getPartyUid() + "*");
			}

			if (collection.getCollectionItem() != null) {
				collection.setCollectionItemId(collection.getCollectionItem().getCollectionItemId());
			}

			resetFolderVisibility(collection.getGooruOid(), user.getPartyUid());
			try {
				if(!collection.getCollectionType().equalsIgnoreCase(ResourceType.Type.ASSESSMENT_URL.getType())) {
					indexHandler.setReIndexRequest(collection.getGooruOid(), IndexProcessor.INDEX, SCOLLECTION, null, false, false);
				}
			} catch (Exception ex) {
				LOGGER.error(_ERROR, ex);
			}
			if (collection.getCollectionType().equalsIgnoreCase(CollectionType.COLLECTION.getCollectionType()) && collection.getUser() != null) {
				final Map<String, String> data = new HashMap<String, String>();
				data.put(EVENT_TYPE, CustomProperties.EventMapping.FIRST_COLLECTION.getEvent());
				data.put(_GOORU_UID, collection.getUser().getGooruUId());
				data.put(ACCOUNT_TYPE_ID, collection.getUser().getAccountTypeId() != null ? collection.getUser().getAccountTypeId().toString() : null);
				// this.getMailAsyncExecutor().handleMailEvent(data);
				this.mailHandler.handleMailEvent(data);

			}
			
			final Set<ContentSettings> contentSettingsObj = new HashSet<ContentSettings>();
			final ContentSettings contentSetting = new ContentSettings();
			contentSetting.setContent(collection);
			if (collection.getSettings() != null && collection.getSettings().size() > 0) {
				contentSetting.setData(new JSONSerializer().exclude("*.class").serialize(collection.getSettings()));
			} else {
				final Map<String, String> map = new HashMap<String, String>();
				map.put("comment", "turn-on");
				contentSetting.setData(new JSONSerializer().exclude("*.class").serialize(map));
			}
			this.getCollectionRepository().save(contentSetting);
			contentSettingsObj.add(contentSetting);
			collection.setContentSettings(contentSettingsObj);
			getAsyncExecutor().deleteFromCache(V2_ORGANIZE_DATA + collection.getUser().getPartyUid() + "*");
			this.getCollectionEventLog().getEventLogs(collection.getCollectionItem(), true, false, user, false, false, null);

		}

		return new ActionResponseDTO<Collection>(collection, errors);
	}

	protected void addCollectionTaxonomy(final Collection collection, final String taxonomyCode, final boolean updateTaxonomyByCode) {
		final String[] taxonomyCodes = taxonomyCode.split(",");
		Set<Code> codes = collection.getTaxonomySet();
		for (final String codeId : taxonomyCodes) {
			Code newCode = null;
			if (updateTaxonomyByCode) {
				newCode = (Code) this.getTaxonomyRepository().findCodeByTaxCode(codeId);
			} else {
				newCode = (Code) this.getTaxonomyRepository().findCodeByCodeId(Integer.parseInt(codeId));
			}
			if (newCode != null) {
				if (codes != null && codes.size() > 0) {
					boolean isExisting = false;
					for (final Code code : codes) {
						if (code.getCodeId().equals(newCode.getCodeId())) {
							isExisting = true;
							break;
						}
					}
					if (!isExisting) {
						codes.add(newCode);
					}
				} else {
					codes = new HashSet<Code>();
					codes.add(newCode);
				}
			}
		}
		collection.setTaxonomySet(codes);
	}

	public ActionResponseDTO<Collection> updateCollection(final Collection newCollection, final String updateCollectionId, final String taxonomyCode, final String ownerUId, String creatorUId, final boolean hasUnrestrictedContentAccess, final String relatedContentId, final boolean updateTaxonomyByCode, final User apiCallerUser)
			throws Exception {
		String gooruUid = null;
		if (newCollection.getUser() != null && !userService.isContentAdmin(newCollection.getUser())) {
			gooruUid = newCollection.getUser().getGooruUId();
		}
		final Collection collection = this.getCollectionByGooruOid(updateCollectionId, gooruUid);
		rejectIfNull(collection, GL0056, COLLECTION);
		final Errors errors = validateUpdateCollection(collection);
		if (!errors.hasErrors()) {

			if (relatedContentId != null) {
				final Content assocContent = this.getContentRepositoryHibernate().findContentByGooruId(relatedContentId);
				if (assocContent != null && collection != null) {
					final ContentAssociation contentAssoc = new ContentAssociation();
					contentAssoc.setAssociateContent(assocContent);
					contentAssoc.setContent(collection);
					contentAssoc.setModifiedDate(new Date());
					contentAssoc.setUser(collection.getUser());
					contentAssoc.setTypeOf(RELATED_CONTENT);
					this.getContentRepositoryHibernate().save(contentAssoc);
					collection.setContentAssociation(contentAssoc);
				}

			}

			if (taxonomyCode != null) {
				addCollectionTaxonomy(collection, taxonomyCode, updateTaxonomyByCode);
				this.getCollectionRepository().save(collection);
			}

			if (newCollection.getVocabulary() != null) {
				collection.setVocabulary(newCollection.getVocabulary());
			}
			if ((newCollection.getBuildType() != null && newCollection.getBuildType().getValue() != null) && (newCollection.getBuildType().getValue() == WEB || newCollection.getBuildType().getValue() == IPAD)) {
				collection.setBuildType(this.getCustomTableRepository().getCustomTableValue(CustomProperties.Table.BUILD_TYPE.getTable(), newCollection.getBuildType().getValue()));
			}

			if (newCollection.getTitle() != null) {
				collection.setTitle(newCollection.getTitle());
			}
			if (newCollection.getDescription() != null) {
				collection.setDescription(newCollection.getDescription());
			}
			if (newCollection.getNarrationLink() != null) {
				collection.setNarrationLink(newCollection.getNarrationLink());
			}
			if (newCollection.getEstimatedTime() != null) {
				collection.setEstimatedTime(newCollection.getEstimatedTime());
			}
			if (newCollection.getNotes() != null) {
				collection.setNotes(newCollection.getNotes());
			}
			if (newCollection.getGoals() != null) {
				collection.setGoals(newCollection.getGoals());
			}
			if (newCollection.getKeyPoints() != null) {
				collection.setGoals(newCollection.getKeyPoints());
			}
			if (newCollection.getLanguage() != null) {
				collection.setLanguage(newCollection.getLanguage());
			}
			if (newCollection.getGrade() != null) {
				collection.setGrade(newCollection.getGrade());
			}
			if (newCollection.getLanguageObjective() != null) {
				collection.setLanguageObjective(newCollection.getLanguageObjective());
			}
			if (newCollection.getIdeas() != null) {
				collection.setIdeas(newCollection.getIdeas());
			}
			if (newCollection.getQuestions() != null) {
				collection.setQuestions(newCollection.getQuestions());
			}
			if (newCollection.getPerformanceTasks() != null) {
				collection.setPerformanceTasks(newCollection.getPerformanceTasks());
			}
			if (collection.getResourceType().getName().equalsIgnoreCase(SCOLLECTION)) {
				if (newCollection.getDepthOfKnowledges() != null && newCollection.getDepthOfKnowledges().size() > 0) {
					collection.setDepthOfKnowledges(this.updateContentMeta(newCollection.getDepthOfKnowledges(), collection, apiCallerUser, DEPTH_OF_KNOWLEDGE));
				} else {
					collection.setDepthOfKnowledges(this.setContentMetaAssociation(this.getContentMetaAssociation(DEPTH_OF_KNOWLEDGE), collection, DEPTH_OF_KNOWLEDGE));
				}

				if (newCollection.getLearningSkills() != null && newCollection.getLearningSkills().size() > 0) {
					collection.setLearningSkills(this.updateContentMeta(newCollection.getLearningSkills(), collection, apiCallerUser, LEARNING_AND_INNOVATION_SKILLS));
				} else {
					collection.setLearningSkills(this.setContentMetaAssociation(this.getContentMetaAssociation(LEARNING_AND_INNOVATION_SKILLS), collection, LEARNING_AND_INNOVATION_SKILLS));
				}

				if (newCollection.getAudience() != null && newCollection.getAudience().size() > 0) {
					collection.setAudience(this.updateContentMeta(newCollection.getAudience(), collection, apiCallerUser, AUDIENCE));
				} else {
					collection.setAudience(this.setContentMetaAssociation(this.getContentMetaAssociation(AUDIENCE), collection, AUDIENCE));
				}

				if (newCollection.getInstructionalMethod() != null && newCollection.getInstructionalMethod().size() > 0) {
					collection.setInstructionalMethod(this.updateContentMeta(newCollection.getInstructionalMethod(), collection, apiCallerUser, INSTRUCTIONAL_METHOD));
				} else {
					collection.setInstructionalMethod(this.setContentMetaAssociation(this.getContentMetaAssociation(INSTRUCTIONAL_METHOD), collection, INSTRUCTIONAL_METHOD));
				}
			}
			if (newCollection.getSharing().equalsIgnoreCase(Sharing.PRIVATE.getSharing()) || newCollection.getSharing().equalsIgnoreCase(Sharing.PUBLIC.getSharing()) || newCollection.getSharing().equalsIgnoreCase(Sharing.ANYONEWITHLINK.getSharing())) {

				if (!newCollection.getSharing().equalsIgnoreCase(PUBLIC)) {
					collection.setPublishStatus(null);
				}
				if (!collection.getCollectionType().equalsIgnoreCase(ResourceType.Type.ASSESSMENT_URL.getType()) && newCollection.getSharing().equalsIgnoreCase(PUBLIC) && !userService.isContentAdmin(apiCallerUser)) {
					collection.setPublishStatus(this.getCustomTableRepository().getCustomTableValue(_PUBLISH_STATUS, PENDING));
					newCollection.setSharing(collection.getSharing());
				}
				if (collection.getCollectionType().equalsIgnoreCase(ResourceType.Type.ASSESSMENT_URL.getType()) || newCollection.getSharing().equalsIgnoreCase(PUBLIC) && userService.isContentAdmin(apiCallerUser)) {
					collection.setPublishStatus(this.getCustomTableRepository().getCustomTableValue(_PUBLISH_STATUS, REVIEWED));
				}
                
				if (collection.getSharing().equalsIgnoreCase(PUBLIC) && newCollection.getSharing().equalsIgnoreCase(Sharing.PRIVATE.getSharing()) || newCollection.getSharing().equalsIgnoreCase(Sharing.ANYONEWITHLINK.getSharing())) {
					final UserSummary userSummary = this.getUserRepository().getSummaryByUid(apiCallerUser.getPartyUid());
					userSummary.setCollections(userSummary.getCollections() <= 0 ? 0 : (userSummary.getCollections() - 1));
					this.getUserRepository().save(userSummary);
				} else if (!collection.getSharing().equalsIgnoreCase(PUBLIC) && newCollection.getSharing().equalsIgnoreCase(PUBLIC)) {
					final UserSummary userSummary = this.getUserRepository().getSummaryByUid(apiCallerUser.getPartyUid());
					if (userSummary.getGooruUid() == null) {
						userSummary.setGooruUid(apiCallerUser.getPartyUid());
					}
					userSummary.setCollections((userSummary.getCollections() != null ? userSummary.getCollections() : 0) + 1);
					this.getUserRepository().save(userSummary);
				}
				collection.setSharing(newCollection.getSharing());

				this.getCollectionRepository().save(collection);
				resetFolderVisibility(collection.getGooruOid(), apiCallerUser.getPartyUid());
				updateResourceSharing(newCollection.getSharing(), collection);
			}

			if (hasUnrestrictedContentAccess) {
				if (creatorUId != null) {
					User user = getUserService().findByGooruId(creatorUId);
					collection.setCreator(user);
				}
				if (ownerUId != null) {
					User user = getUserService().findByGooruId(ownerUId);
					collection.setUser(user);
				}
				if (newCollection.getNetwork() != null) {
					collection.setNetwork(newCollection.getNetwork());
				}
			}

			if (newCollection.getMailNotification() != null) {
				collection.setMailNotification(newCollection.getMailNotification());
			}

			this.getCollectionRepository().save(collection);
			try {
				indexHandler.setReIndexRequest(collection.getGooruOid(), IndexProcessor.INDEX, SCOLLECTION, null, false, false);						
			} catch (Exception e) {
				LOGGER.error(_ERROR, e);
			}
		}
		getAsyncExecutor().deleteFromCache(V2_ORGANIZE_DATA + collection.getUser().getPartyUid() + "*");
		return new ActionResponseDTO<Collection>(collection, errors);
	}

	@Override
	public List<ContentMetaDTO> updateContentMeta(List<ContentMetaDTO> newDepthOfKnowledges, final String collectionId, final User apiCaller, final String type) {
		final Resource resource = this.getResourceRepository().findResourceByContentGooruId(collectionId);
		return updateContentMeta(newDepthOfKnowledges, resource, apiCaller, type);
	}

	@Override
	public List<ContentMetaDTO> updateContentMeta(final List<ContentMetaDTO> newDepthOfKnowledges, final Resource resource, final User apiCaller, final String type) {
		final List<ContentMetaAssociation> removeAll = new ArrayList<ContentMetaAssociation>();
		final List<ContentMetaAssociation> saveAll = new ArrayList<ContentMetaAssociation>();
		final Set<ContentMetaAssociation> updateContent = new HashSet<ContentMetaAssociation>();
		for (final ContentMetaDTO newMeta : newDepthOfKnowledges) {
			final CustomTableValue customTableValue = this.getCustomTableRepository().getValueByDisplayName(newMeta.getValue(), type);
			if (customTableValue != null) {
				Boolean isAlreadyUpdated = false;
				if (resource.getContentMetaAssoc() != null) {
					for (final ContentMetaAssociation contentMetaAssociation : resource.getContentMetaAssoc()) {
						if (contentMetaAssociation.getAssociationType().getDisplayName().equalsIgnoreCase(newMeta.getValue()) && !newMeta.getSelected()) {
							removeAll.add(contentMetaAssociation);
							break;
						} else if (contentMetaAssociation.getAssociationType().getDisplayName().equalsIgnoreCase(newMeta.getValue())) {
							isAlreadyUpdated = true;
							updateContent.add(contentMetaAssociation);
							break;
						}
					}
				}
				if (newMeta.getSelected() && !isAlreadyUpdated) {
					ContentMetaAssociation contentMetaAssociationNew = new ContentMetaAssociation();
					contentMetaAssociationNew.setAssociationType(customTableValue);
					contentMetaAssociationNew.setUser(apiCaller);
					contentMetaAssociationNew.setContent(resource);
					contentMetaAssociationNew.setAssociatedDate(new Date(System.currentTimeMillis()));
					saveAll.add(contentMetaAssociationNew);
					updateContent.add(contentMetaAssociationNew);
				}
			}
		}
		if (saveAll.size() > 0) {
			this.getCollectionRepository().saveAll(saveAll);
		}
		if (removeAll.size() > 0) {
			this.getCollectionRepository().removeAll(removeAll);
		}
		return this.setContentMetaAssociation(this.getContentMetaAssociation(type), updateContent, type);
	}

	@Override
	public void deleteCollection(final String collectionId, final User user) {
		final Collection collection = this.getCollectionByGooruOid(collectionId, null);
		rejectIfNull(collection, GL0056, _COLLECTION);
		if (this.getOperationAuthorizer().hasUnrestrictedContentAccess(collectionId, user)) {
			List<CollectionItem> collectionitems = this.getCollectionRepository().getCollectionItemsByResource(collectionId);
			for (CollectionItem collectionItem : collectionitems) {
				List<CollectionItem> resetCollectionItems = this.getCollectionRepository().getResetSequenceCollectionItems(collectionItem.getCollection().getGooruOid(), collectionItem.getItemSequence());
				int itemSequence = collectionItem.getItemSequence();
				for (CollectionItem resetCollectionItem : resetCollectionItems) { 
					resetCollectionItem.setItemSequence(itemSequence++);
				}
				this.getCollectionRepository().saveAll(resetCollectionItems);
			} 
			final List<CollectionItem> collectionItems = this.getCollectionRepository().getCollectionItemByAssociation(collectionId, null, null);
			final List<CollectionItem> parentAssociations = this.getCollectionRepository().getCollectionItemByParentId(collectionId, null, null);
			if (parentAssociations != null && parentAssociations.size() > 0) {
				collectionItems.addAll(parentAssociations);
			}
			try {
				this.getCollectionEventLog().getEventLogs(collection.getCollectionItem(), user, collection.getCollectionType());
			} catch (Exception e) {
				LOGGER.error(_ERROR, e);
			} 
			if (collection != null && collection.getUser() != null && collection.getSharing().equalsIgnoreCase(PUBLIC) && !collection.getCollectionType().equalsIgnoreCase(ResourceType.Type.PATHWAY.getType())) {
				final UserSummary userSummary = this.getUserRepository().getSummaryByUid(collection.getUser().getPartyUid());
				if (userSummary != null && userSummary.getCollections() != null) {
					userSummary.setCollections(userSummary.getCollections() <= 0 ? 0 : (userSummary.getCollections() - 1));
					this.getUserRepository().save(userSummary);
				}
			}
			try {
				if(!collection.getCollectionType().equalsIgnoreCase(ResourceType.Type.ASSESSMENT_URL.getType())) {
					indexHandler.setReIndexRequest(collection.getGooruOid(), IndexProcessor.DELETE, SCOLLECTION, null, false, false);
				}
			} catch(Exception e) { 
				LOGGER.error(_ERROR, e);
			}
			this.getCollectionRepository().remove(collection);

			for (final CollectionItem item : collectionItems) {
				if (item.getAssociatedUser() != null && !item.getAssociatedUser().getPartyUid().equals(user.getPartyUid())) {
					getAsyncExecutor().deleteFromCache(V2_ORGANIZE_DATA + item.getAssociatedUser().getPartyUid() + "*");
				}
				final Collection parentCollection = item.getCollection();
				if (parentCollection.getCollectionType().equals(FOLDER)) {
					updateFolderSharing(parentCollection.getGooruOid());
					resetFolderVisibility(parentCollection.getGooruOid(), collection.getUser().getPartyUid());
				}
				this.deleteCollectionItem(item.getCollectionItemId());
			}
		} else {
			throw new UnauthorizedException(generateErrorMessage(GL0099, _COLLECTION));
		}
		getAsyncExecutor().deleteFromCache(V2_ORGANIZE_DATA + user.getPartyUid() + "*");
		getAsyncExecutor().deleteFromCache("v2-class-data-*");
		
	}

	@Override
	public List<Collection> getCollections(final Map<String, String> filters, final User user) {
		return this.getCollectionRepository().getCollections(filters, user);
	}

	@Override
	public ActionResponseDTO<CollectionItem> createCollectionItem(String resourceGooruOid, String collectionGooruOid, CollectionItem collectionItem, User user, String type, boolean isCreateQuestion) throws Exception {
		final Collection collection = this.createMyShelfCollection(collectionGooruOid, user, type, collectionItem);
		collectionItem.setAssociatedUser(user);
		collectionItem.setAssociationDate(new Date(System.currentTimeMillis()));
		final Resource resource = this.getResourceRepository().findResourceByContentGooruId(resourceGooruOid);
		final Errors errors = validateCollectionItem(collection, resource, collectionItem);
		if (!errors.hasErrors()) {
			collectionItem.setCollection(collection);

			if (collectionItem.getCollection() != null) {
				collectionItem.getCollection().setLastUpdatedUserUid(user.getPartyUid());
			}

			if (!isCreateQuestion && resource != null && resource.getResourceType().getName().equalsIgnoreCase(ResourceType.Type.ASSESSMENT_QUESTION.getType())) {
				final AssessmentQuestion assessmentQuestion = assessmentService.copyAssessmentQuestion(user, resource.getGooruOid());
				collectionItem.setResource(assessmentQuestion);
			} else {
				collectionItem.setResource(resource);
			}

			int sequence = collectionItem.getCollection().getCollectionItems() != null ? collectionItem.getCollection().getCollectionItems().size() + 1 : 1;
			collectionItem.setItemSequence(sequence);
			collectionItem.getCollection().setItemCount(sequence);
			this.getCollectionRepository().save(collectionItem);
				this.getCollectionEventLog().getEventLogs(collectionItem, true, false, user, false, false, null);

			try {
				if (collectionItem.getResource().getResourceType() != null && !collectionItem.getResource().getResourceType().getName().equalsIgnoreCase(SCOLLECTION) && !collectionItem.getResource().getResourceType().getName().equalsIgnoreCase(FOLDER)
						&& !collectionItem.getResource().getResourceType().getName().equalsIgnoreCase(CLASSPAGE)) {
					indexHandler.setReIndexRequest(collectionItem.getResource().getGooruOid(), IndexProcessor.INDEX, RESOURCE, null, false, false);							
				}
				if (!collectionItem.getCollection().getResourceType().getName().equalsIgnoreCase(SHELF)) {
					indexHandler.setReIndexRequest(collectionItem.getCollection().getGooruOid(), IndexProcessor.INDEX, SCOLLECTION, null, false, false);							
				}

			} catch (Exception e) {
				LOGGER.error(_ERROR, e);
			}
			resetFolderVisibility(collection.getGooruOid(), collection.getUser().getPartyUid());
			if (collectionItem.getCollection().getResourceType().getName().equalsIgnoreCase(SCOLLECTION) && collectionItem.getCollection().getClusterUid() != null && !collectionItem.getCollection().getClusterUid().equalsIgnoreCase(collectionItem.getCollection().getGooruOid())) {
				collectionItem.getCollection().setClusterUid(collectionItem.getCollection().getGooruOid());
				this.getCollectionRepository().save(collectionItem.getCollection());
			}
			getAsyncExecutor().deleteFromCache(V2_ORGANIZE_DATA + collectionItem.getCollection().getUser().getPartyUid() + "*");
			getAsyncExecutor().deleteFromCache("v2-class-data-" + collection.getGooruOid() + "*");
		}

		return new ActionResponseDTO<CollectionItem>(collectionItem, errors);
	}

	private Collection createMyShelfCollection(final String collectionGooruOid, final User user, final String type, final CollectionItem collectionItem) {

		Collection collection = null;
		if (type != null && type.equalsIgnoreCase(CollectionType.SHElf.getCollectionType())) {
			collectionItem.setItemType(ShelfType.AddedType.SUBSCRIBED.getAddedType());
		} else if (type != null && type.equalsIgnoreCase(COLLABORATOR)) {
			collectionItem.setItemType(COLLABORATOR);
		} else if (type != null && type.equalsIgnoreCase(CLASS)) {
			collectionItem.setItemType(CLASS);
		} else {
			if (collectionItem != null && collectionItem.getItemType() == null) {
				collectionItem.setItemType(ShelfType.AddedType.ADDED.getAddedType());
			}
		}
		if (collectionGooruOid != null) {
			collection = this.getCollectionByGooruOid(collectionGooruOid, null);
		} else {
			collection = this.getCollectionRepository().getUserShelfByGooruUid(user.getGooruUId(), CollectionType.SHElf.getCollectionType());
		}
		if (collection == null) {
			collection = new Collection();
			collection.setTitle(MY_SHELF);
			collection.setCollectionType(CollectionType.SHElf.getCollectionType());
			collection.setGooruOid(UUID.randomUUID().toString());
			ContentType contentType = (ContentType) this.getCollectionRepository().get(ContentType.class, ContentType.RESOURCE);
			collection.setContentType(contentType);
			final ResourceType resourceType = (ResourceType) this.getCollectionRepository().get(ResourceType.class, ResourceType.Type.SHELF.getType());
			collection.setResourceType(resourceType);
			collection.setLastModified(new Date(System.currentTimeMillis()));
			collection.setCreatedOn(new Date(System.currentTimeMillis()));
			collection.setSharing(Sharing.PRIVATE.getSharing());
			collection.setUser(user);
			collection.setOrganization(user.getPrimaryOrganization());
			collection.setCreator(user);
			collection.setDistinguish(Short.valueOf(ZERO));
			collection.setRecordSource(NOT_ADDED);
			collection.setIsFeatured(0);
			this.getCollectionRepository().save(collection);
		}
		return collection;
	}

	@Override
	public ActionResponseDTO<CollectionItem> updateCollectionItem(final CollectionItem newcollectionItem, final String collectionItemId, final User user, final String data) throws Exception {
		final CollectionItem collectionItem = this.getCollectionItemById(collectionItemId);
		if (collectionItem == null) {
			throw new BadRequestException("Item not found");
		}
		final Errors errors = validateUpdateCollectionItem(newcollectionItem);

		if (!errors.hasErrors()) {
			if (newcollectionItem.getNarration() != null) {
				collectionItem.setNarration(newcollectionItem.getNarration());
			}
			if (newcollectionItem.getIsRequired() != null) {
				collectionItem.setIsRequired(newcollectionItem.getIsRequired());
			}
			if (newcollectionItem.getShowAnswerByQuestions() != null) {
				collectionItem.setShowAnswerByQuestions(newcollectionItem.getShowAnswerByQuestions());
			}
			if (newcollectionItem.getShowAnswerEnd() != null) {
				collectionItem.setShowAnswerEnd(newcollectionItem.getShowAnswerEnd());
			}
			if (newcollectionItem.getShowHints() != null) {
				collectionItem.setShowHints(newcollectionItem.getShowHints());
			}
			if (newcollectionItem.getMinimumScore() != null) {
				collectionItem.setMinimumScore(newcollectionItem.getMinimumScore());
			}
			if (newcollectionItem.getEstimatedTime() != null) {
				collectionItem.setEstimatedTime(newcollectionItem.getEstimatedTime());
			}
			if (newcollectionItem.getPlannedEndDate() != null) {
				collectionItem.setPlannedEndDate(newcollectionItem.getPlannedEndDate());
			}
			if (newcollectionItem.getNarrationType() != null) {
				collectionItem.setNarrationType(newcollectionItem.getNarrationType());
			}
			if (newcollectionItem.getStart() != null) {
				collectionItem.setStart(newcollectionItem.getStart());
			}
			if (newcollectionItem.getStop() != null) {
				collectionItem.setStop(newcollectionItem.getStop());
			}
			if (collectionItem.getResource() != null && collectionItem.getResource().getResourceType() != null && collectionItem.getResource().getResourceType().getName().equalsIgnoreCase(ResourceType.Type.SCOLLECTION.getType())) {
				collectionItem.getResource().setLastUpdatedUserUid(user.getPartyUid());
			}
			this.getCollectionRepository().save(collectionItem);
			this.getCollectionRepository().save(collectionItem.getCollection());
			try {
				this.getCollectionEventLog().getEventLogs(collectionItem, false, false, user, false, false, SerializerUtil.serializeToJson(newcollectionItem, EXCLUDE, true, true));
			} catch (Exception e) {
				LOGGER.error(_ERROR, e);
			}
			if (collectionItem.getResource().getResourceType() != null && collectionItem.getResource().getResourceType().getName().equalsIgnoreCase(ResourceType.Type.SCOLLECTION.getType())) {
					indexHandler.setReIndexRequest(collectionItem.getResource().getGooruOid(), IndexProcessor.INDEX, SCOLLECTION, null, false, false);							
				} else {
					indexHandler.setReIndexRequest(collectionItem.getResource().getGooruOid(), IndexProcessor.INDEX, RESOURCE, null, false, false);							
				}
				indexHandler.setReIndexRequest(collectionItem.getCollection().getGooruOid(), IndexProcessor.INDEX, SCOLLECTION, null, false, false);						
				getAsyncExecutor().deleteFromCache(V2_ORGANIZE_DATA + collectionItem.getCollection().getUser().getPartyUid() + "*");
				getAsyncExecutor().deleteFromCache("v2-class-data-" + collectionItem.getCollection().getGooruOid() + "*");
		}

		return new ActionResponseDTO<CollectionItem>(collectionItem, errors);
	}

	@Override
	public List<CollectionItem> getCollectionItems(final String collectionId, Map<String, String> filters) {
		return this.getCollectionRepository().getCollectionItems(collectionId, filters);
	}

	@Override
	public CollectionItem getCollectionItem(final String collectionItemId, final boolean includeAdditionalInfo, final User user, final String rootNodeId) {
		CollectionItem collectionItem = this.getCollectionRepository().getCollectionItemById(collectionItemId);
		rejectIfNull(collectionItem, GL0056, _COLLECTION_ITEM);
		if (user != null) {
			final UserCollectionItemAssoc userCollectionItemAssoc = this.getCollectionRepository().getUserCollectionItemAssoc(collectionItemId, user.getPartyUid());
			if (userCollectionItemAssoc != null) {
				if (userCollectionItemAssoc.getStatus() != null) {
					collectionItem.setStatus(userCollectionItemAssoc.getStatus().getValue());
				}
				if (userCollectionItemAssoc.getMinimumScore() != null) {
					collectionItem.setMinimumScoreByUser(userCollectionItemAssoc.getMinimumScore());
				}
				collectionItem.setAssignmentCompleted(userCollectionItemAssoc.getAssignmentCompleted());
				collectionItem.setTimeStudying(userCollectionItemAssoc.getTimeStudying());
			}
		}
		if (includeAdditionalInfo) {
			collectionItem = this.setCollectionItemMoreData(collectionItem, rootNodeId);
		}
		return collectionItem;
	}

	@Override
	public void deleteCollectionItem(final String collectionItemId, final User user, boolean indexCollection) {
		final CollectionItem collectionItem = this.getCollectionRepository().getCollectionItemById(collectionItemId);
		if (collectionItem != null && collectionItem.getResource() != null) {
			try {
				this.getCollectionEventLog().getEventLogs(collectionItem, user, collectionItem.getCollection().getCollectionType());
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
			final Collection collection = collectionItem.getCollection();
			final Resource resource = collectionItem.getResource();
			this.getCollectionRepository().remove(CollectionItem.class, collectionItem.getCollectionItemId());

			collection.setLastUpdatedUserUid(user.getPartyUid());
			collection.setLastModified(new Date(System.currentTimeMillis()));
			if (collection.getResourceType().getName().equalsIgnoreCase(SCOLLECTION) && collection.getClusterUid() != null && !collection.getClusterUid().equalsIgnoreCase(collection.getGooruOid())) {
				collection.setClusterUid(collectionItem.getCollection().getGooruOid());
			}
			collection.setItemCount((collection.getItemCount() == null || (collection.getItemCount() != null && collection.getItemCount() == 0)) ? 0 : collection.getItemCount() - 1);
			reOrderCollectionItems(collection, collectionItemId);		
			this.getCollectionRepository().save(collection);
			try {
				if (resource.getResourceType() != null && resource.getResourceType().getName().equalsIgnoreCase(ResourceType.Type.SCOLLECTION.getType())) {
					indexHandler.setReIndexRequest(resource.getGooruOid(), IndexProcessor.DELETE, SCOLLECTION, null, false, false);							
				} else {
					indexHandler.setReIndexRequest(resource.getGooruOid(), IndexProcessor.INDEX, RESOURCE, null, false, false);							
				}
				if (indexCollection) {
					indexHandler.setReIndexRequest(collection.getGooruOid(), IndexProcessor.INDEX, SCOLLECTION, null, false, false);							
				}
				getAsyncExecutor().deleteFromCache(V2_ORGANIZE_DATA + collection.getUser().getPartyUid() + "*");
				getAsyncExecutor().deleteFromCache("v2-class-data-" + collection.getGooruOid() + "*");
			} catch (Exception e) {
				LOGGER.error(_ERROR, e);
			}
		} else {
			throw new NotFoundException(generateErrorMessage(GL0056, _COLLECTION_ITEM), GL0056);
		}
	}
	
	@Override
	public void deleteCollectionItem(String collectionItemId) {
		final CollectionItem collectionItem = this.getCollectionRepository().getCollectionItemById(collectionItemId);
		if (collectionItem != null) {
			this.getCollectionRepository().remove(CollectionItem.class, collectionItem.getCollectionItemId());
		}
		try {
			indexHandler.setReIndexRequest(collectionItem.getResource().getGooruOid(), IndexProcessor.INDEX, RESOURCE, null, false, false);						
		} catch(Exception e) { 
			LOGGER.error(_ERROR, e);
		}
	}

	@Override
	public ActionResponseDTO<CollectionItem> reorderCollectionItem(final String collectionItemId, final int newSequence, final User user) throws Exception {
		final CollectionItem collectionItem = getCollectionRepository().getCollectionItemById(collectionItemId);
		if (collectionItem == null) {
			throw new BadRequestException(generateErrorMessage(GL0056, COLLECTION_ITEM), GL0056);
		}
		Errors errors = validateReorderCollectionItem(collectionItem);
		if (!errors.hasErrors()) {
			final Collection collection = getCollectionRepository().getCollectionByGooruOid(collectionItem.getCollection().getGooruOid(), null);

			final Integer existCollectionItemSequence = collectionItem.getItemSequence();

			if (existCollectionItemSequence > newSequence) {
				for (CollectionItem ci : collection.getCollectionItems()) {

					if (ci.getItemSequence() >= newSequence && ci.getItemSequence() <= existCollectionItemSequence) {
						if (ci.getCollectionItemId().equalsIgnoreCase(collectionItem.getCollectionItemId())) {
							ci.setItemSequence(newSequence);
						} else {
							ci.setItemSequence(ci.getItemSequence() + 1);
						}
					}
				}

			} else if (existCollectionItemSequence < newSequence) {
				for (final CollectionItem ci : collection.getCollectionItems()) {
					if (ci.getItemSequence() <= newSequence && existCollectionItemSequence <= ci.getItemSequence()) {
						if (ci.getCollectionItemId().equalsIgnoreCase(collectionItem.getCollectionItemId())) {
							if (collection.getCollectionItems().size() < newSequence) {
								ci.setItemSequence(collection.getCollectionItems().size());
							} else {
								ci.setItemSequence(newSequence);
							}
						} else {
							ci.setItemSequence(ci.getItemSequence() - 1);
						}
					}
				}
			}
			this.getCollectionRepository().save(collection);
			this.getClasspageEventLog().getEventLogs(collectionItem, collectionItem.getResource().getGooruOid(), user, collectionItem, collection.getCollectionType());
			getAsyncExecutor().deleteFromCache(V2_ORGANIZE_DATA + collection.getUser().getPartyUid() + "*");
			getAsyncExecutor().deleteFromCache("v2-class-data-" + collection.getGooruOid() + "*");
		}
		return new ActionResponseDTO<CollectionItem>(collectionItem, errors);
	}

	@Override
	public String getCollectionWithCache(final String collectionId, final boolean includeMetaInfo, final boolean includeCollaborator, final boolean isContentFlag, final User user, final String merge, final String rootNodeId, final boolean isGat, final boolean includeCollectionItem, final boolean includeRelatedContent, final boolean clearCache) {
		final String cacheKey = "v2-collection-data-" + collectionId + "-" + includeMetaInfo + "-" + includeCollaborator + "-" + isContentFlag;
		Map<String, Object> cacheCollection = null;
		final boolean isCollaborator = false;
		String data = null;
		if (!clearCache) {
			data = this.redisService.getValue(cacheKey);
		}
		if (data == null) {
			Collection collection = getCollection(collectionId, includeMetaInfo, includeCollaborator, isContentFlag, user, merge, rootNodeId, isGat, true, true, true);
			data = SerializerUtil.serialize(collection, FORMAT_JSON, EXCLUDE_ALL, false, true, includes(includeCollectionItem, includeMetaInfo, includeRelatedContent));
			redisService.putValue(cacheKey, data);
		} else {
			cacheCollection = JsonDeserializer.deserialize(data, new TypeReference<Map<String, Object>>() {
			});
			if (cacheCollection != null) {
				
				if (merge != null) {
					final Map<String, Object> permissions = new HashMap<String, Object>();
					if (merge.contains(PERMISSIONS)) {
						permissions.put(PERMISSIONS, this.getContentService().getContentPermission(collectionId, user));
					}
					if (merge.contains(REACTION_AGGREGATE)) {
						permissions.put(REACTION_AGGREGATE, this.getFeedbackService().getContentFeedbackAggregate(collectionId, REACTION));
					}
					if (merge.contains(COMMENT_COUNT)) {
						permissions.put(COMMENT_COUNT, this.getCommentRepository().getCommentCount(null, collectionId, null, "notdeleted"));
					}
					final long collaboratorCount = this.getCollaboratorRepository().getCollaboratorsCountById(collectionId);
					permissions.put(COLLABORATOR_COUNT, collaboratorCount);
					permissions.put(IS_COLLABORATOR, isCollaborator);
					cacheCollection.put(META, permissions);
					data = SerializerUtil.serialize(cacheCollection, FORMAT_JSON, EXCLUDE_ALL, false, true, includes(includeCollectionItem, includeMetaInfo, includeRelatedContent));
				}
			}
		}

		return data;
	}

	private String[] includes(final boolean includeCollectionItem, final boolean includeMetaInfo, final boolean includeRelatedContent) {
		String includes[] = (String[]) ArrayUtils.addAll(RESOURCE_INCLUDE_FIELDS, COLLECTION_INCLUDE_FIELDS);
		if (includeCollectionItem) {
			includes = (String[]) ArrayUtils.addAll(includes, COLLECTION_ITEM_INCLUDE_FILEDS);
		}
		if (includeMetaInfo) {
			includes = (String[]) ArrayUtils.addAll(includes, COLLECTION_META_INFO);
		}
		includes = (String[]) ArrayUtils.addAll(includes, COLLECTION_TAXONOMY);

		if (includeRelatedContent) {
			includes = (String[]) ArrayUtils.add(includes, "*.contentAssociation");
		}
		includes = (String[]) ArrayUtils.addAll(includes, COLLECTION_ITEM_TAGS);
		return includes;
	}

	@Override
	public Collection getCollection(final String collectionId, final boolean includeMetaInfo, final boolean includeCollaborator, final boolean isContentFlag, final User user, final String merge, final String rootNodeId, final boolean isGat, final boolean includeViewCount, final boolean includeContentProvider, final boolean includeCustomFields) {
		final Collection collection = this.getCollectionRepository().getCollectionByGooruOid(collectionId, null);
		final boolean isCollaborator = this.getCollaboratorRepository().findCollaboratorById(collectionId, user.getGooruUId()) != null ? true : false;
		if (collection != null && (collection.getUser().getGooruUId().equalsIgnoreCase(user.getGooruUId()) || !collection.getSharing().equalsIgnoreCase(Sharing.PRIVATE.getSharing()) || userService.isContentAdmin(user) || isCollaborator)) {
			if (includeMetaInfo) {
				this.setCollectionMetaData(collection, user, merge, false, rootNodeId, includeViewCount, includeContentProvider, includeCustomFields);
			}
			if (isGat) {
				collection.setTaxonomySetMapping(TaxonomyUtil.getTaxonomyByCode(collection.getTaxonomySet(), taxonomyService));
			}
			if (collection.getUser() != null) {
				collection.getUser().setProfileImageUrl(this.getUserService().buildUserProfileImageUrl(collection.getUser()));
			}
			if (isContentFlag) {
				collection.setContentAssociation(this.contentAssociationRepository.getContentAssociationGooruOid(collectionId));
			}
			if (collection.getUser().getIdentities() != null) {
				final Identity identity = collection.getUser().getIdentities().iterator().next();
				collection.getUser().setEmailId(identity.getExternalId());
			}
			final User lastUserModified = this.getUserService().findByGooruId(collection.getLastUpdatedUserUid());
			final Map<String, Object> lastUserModifiedMap = new HashMap<String, Object>();
			if (lastUserModified != null) {
				lastUserModifiedMap.put(USER_NAME, lastUserModified.getUsername());
				lastUserModifiedMap.put(GOORU_UID, lastUserModified.getGooruUId());
			}
			setView(collection);
			collection.setLastModifiedUser(lastUserModifiedMap);
			if (collection.getResourceType().getName().equalsIgnoreCase(SCOLLECTION)) {
				collection.setDepthOfKnowledges(this.setContentMetaAssociation(this.getContentMetaAssociation(DEPTH_OF_KNOWLEDGE), collection, DEPTH_OF_KNOWLEDGE));
				collection.setLearningSkills(this.setContentMetaAssociation(this.getContentMetaAssociation(LEARNING_AND_INNOVATION_SKILLS), collection, LEARNING_AND_INNOVATION_SKILLS));
				collection.setAudience(this.setContentMetaAssociation(this.getContentMetaAssociation(AUDIENCE), collection, AUDIENCE));
				collection.setInstructionalMethod(this.setContentMetaAssociation(this.getContentMetaAssociation(INSTRUCTIONAL_METHOD), collection, INSTRUCTIONAL_METHOD));
			}
			if (merge != null) {
				final Map<String, Object> permissions = new HashMap<String, Object>();
				if (merge.contains(PERMISSIONS)) {
					permissions.put(PERMISSIONS, this.getContentService().getContentPermission(collection, user));
				}
				if (merge.contains(REACTION_AGGREGATE)) {
					permissions.put(REACTION_AGGREGATE, this.getFeedbackService().getContentFeedbackAggregate(collectionId, REACTION));
				}
				if (merge.contains(COMMENT_COUNT)) {
					permissions.put(COMMENT_COUNT, this.getCommentRepository().getCommentCount(null, collection.getGooruOid(), null, "notdeleted"));
				}
				final long collaboratorCount = this.getCollaboratorRepository().getCollaboratorsCountById(collectionId);
				permissions.put(COLLABORATOR_COUNT, collaboratorCount);
				permissions.put(IS_COLLABORATOR, isCollaborator);
				collection.setMeta(permissions);
				if (includeCustomFields) {
					collection.setCustomFieldValues(this.getCustomFieldsService().getCustomFieldsValuesOfResource(collection.getGooruOid()));
				}
			}
		} else {
			throw new NotFoundException(generateErrorMessage(GL0056, _COLLECTION), GL0056);
		}
		return collection;
	}

	@Override
	public Map<String, Object> setRatingsObj(final ResourceSummary resourceSummary) {
		final Map<String, Object> ratings = new HashMap<String, Object>();
		if (resourceSummary != null) {
			ratings.put(AVERAGE, resourceSummary.getRatingStarAvg());
			ratings.put(COUNT, resourceSummary.getRatingStarCount());
			ratings.put("reviewCount", resourceSummary.getReviewCount());
		}
		return ratings;
	}

	@Override
	public List<ContentMetaDTO> getContentMetaAssociation(final String type) {
		List<ContentMetaDTO> depthOfKnowledges = new ArrayList<ContentMetaDTO>();
		final String cacheKey = "content_meta_association_type_" + type;
		final String data = redisService.getValue(cacheKey);
		if (data == null) {
			final List<CustomTableValue> customTableValues = this.getCustomTableRepository().getFilterValueFromCustomTable(type);
			for (CustomTableValue customTableValue : customTableValues) {
				depthOfKnowledges.add(new ContentMetaDTO(customTableValue.getValue(),customTableValue.getDisplayName(), false));
			}
			redisService.putValue(cacheKey, JsonSerializer.serialize(depthOfKnowledges, FORMAT_JSON));
		} else {
			depthOfKnowledges = JsonDeserializer.deserialize(data, new TypeReference<List<ContentMetaDTO>>() {
			});
		}
		return depthOfKnowledges;
	}

	@Override
	public List<ContentMetaDTO> setContentMetaAssociation(List<ContentMetaDTO> depthOfKnowledges, String collectionId, final String type) {
		final Resource resource = this.getResourceRepository().findResourceByContent(collectionId);
		rejectIfNull(resource, "GL0056", RESOURCE + " ID: " + collectionId);
		return setContentMetaAssociation(depthOfKnowledges, resource.getContentMetaAssoc(), type);
	}

	@Override
	public List<ContentMetaDTO> setContentMetaAssociation(List<ContentMetaDTO> depthOfKnowledges, Resource resource, String type) {
		return setContentMetaAssociation(depthOfKnowledges, resource.getContentMetaAssoc(), type);
	}

	public List<ContentMetaDTO> setContentMetaAssociation(List<ContentMetaDTO> depthOfKnowledges, Set<ContentMetaAssociation> contentMetaAssociations, final String type) {
		if (contentMetaAssociations != null && contentMetaAssociations.size() > 0) {
			for (ContentMetaAssociation contentMetaAssociation : contentMetaAssociations) {
				for (ContentMetaDTO depthOfKnowledge : depthOfKnowledges) {
					if (depthOfKnowledge.getValue().equalsIgnoreCase(contentMetaAssociation.getAssociationType().getDisplayName())) {
						depthOfKnowledge.setSelected(true);
					}
				}
			}
		}
		return depthOfKnowledges;
	}

	@Override
	public Collection copyCollection(final String collectionId, final String title, boolean addToShelf, User user, String taxonomyCode, final String grade, String parentId) throws Exception {
		final Collection newCollection = new Collection();
		newCollection.setTitle(title);
		newCollection.setUser(user);
		newCollection.setGrade(grade);
		if (taxonomyCode != null) {
			addCollectionTaxonomy(newCollection, taxonomyCode, false);
		}
		return this.copyCollection(collectionId, newCollection, addToShelf, parentId, user);
	}

	@Override
	public User addCollaborator(final String collectionId, final User user, final String collaboratorId, final String collaboratorOperation) {
		final Collection collection = this.getCollectionByGooruOid(collectionId, null);
		rejectIfNull(collection, GL0056, _COLLECTION);
		if (collaboratorId != null) {
			final List<String> collaboratorsList = Arrays.asList(collaboratorId.split("\\s*,\\s*"));
			for (User collaborator : getUserService().findByIdentities(collaboratorsList)) {
				if (getUserService().checkCollaboratorsPermission(collectionId, collaborator, SCOLLECTION)) {
					return collectionUtil.updateNewCollaborators(collection, collaboratorsList, user, COLLECTION_COLLABORATE, collaboratorOperation);
				} else {
					throw new NotFoundException(generateErrorMessage("GL0006"), "GL0006");
				}
			}
		}

		return null;
	}

	@Override
	public List<User> getCollaborators(String collectionId) {
		final Collection collection = this.getCollectionByGooruOid(collectionId, null);
		rejectIfNull(collection, GL0056, _COLLECTION);
		return this.getCollaboratorRepository().findCollaborators(collectionId, null);
	}

	private Collection setCollectionMetaData(Collection collection, final User user, final String merge, boolean ignoreUserTaxonomyPreference, String rootNodeId, boolean includeViewCount, boolean includeContentProvider, boolean includeCustomFields) {
		if (collection != null) {
			final Set<String> acknowledgement = new HashSet<String>();
			final ResourceMetaInfo collectionMetaInfo = new ResourceMetaInfo();
			setCollectionTaxonomyMetaInfo(collection.getTaxonomySet(), collectionMetaInfo);
			collectionMetaInfo.setStandards(this.getStandards(collection.getTaxonomySet(), ignoreUserTaxonomyPreference, rootNodeId));
			if (collection.getVocabulary() != null) {
				collectionMetaInfo.setVocabulary(Arrays.asList(collection.getVocabulary().split("\\s*,\\s*")));
			}
			collection.setMetaInfo(collectionMetaInfo);
			if (collection.getCollectionItems() != null) {
				for (final CollectionItem collectionItem : collection.getCollectionItems()) {
					Resource resource = collectionItem.getResource();
					if (resource != null && resource.getResourceSource() != null && resource.getResourceSource().getAttribution() != null) {
						acknowledgement.add(resource.getResourceSource().getAttribution());
						if (resource.getResourceType().getName().equalsIgnoreCase(ResourceType.Type.VIDEO.getType())) {
							final String duration = getResourceCassandraService().get(resource.getGooruOid(), RESOURCE_METADATA_DURATION);
							if (duration != null) {
								resource.setDurationInSec(duration);
							}
						}
					}
					resource.setEducationalUse(this.setContentMetaAssociation(this.getContentMetaAssociation(EDUCATIONAL_USE), resource, EDUCATIONAL_USE));
					resource.setRatings(this.setRatingsObj(this.getResourceRepository().getResourceSummaryById(resource.getGooruOid())));
					
					if (includeCustomFields) {
						resource.setCustomFieldValues(this.getCustomFieldsService().getCustomFieldsValuesOfResource(resource.getGooruOid()));
					}
					if (includeContentProvider) {
						resource = getResourceService().setContentProvider(resource);
					}
					
					resource.setResourceTags(this.getContentService().getContentTagAssoc(resource.getGooruOid(), user));
					if (resource.getResourceType().getName().equalsIgnoreCase(ASSESSMENT_QUESTION)) {
						resource.setDepthOfKnowledges(this.setContentMetaAssociation(this.getContentMetaAssociation(DEPTH_OF_KNOWLEDGE), resource, DEPTH_OF_KNOWLEDGE));
					} else {
						resource.setMomentsOfLearning(this.setContentMetaAssociation(this.getContentMetaAssociation(MOMENTS_OF_LEARNING), resource, MOMENTS_OF_LEARNING));
					}
					
					if ((merge != null && merge.contains(REACTION)) && (resource != null)) {
						Map<String, Object> resourcePermissions = new HashMap<String, Object>();
						resourcePermissions.put(REACTION, this.getFeedbackService().getContentFeedbacks(REACTION, null, collectionItem.getResource().getGooruOid(), collection.getUser().getPartyUid(), null, null, null));
						resource.setMeta(resourcePermissions);
					}
					setView(resource);
					resource.setSkills(getSkills(resource.getTaxonomySet()));
					collectionItem.setResource(resource);
					this.setCollectionItemMoreData(collectionItem, rootNodeId);
				}
				collectionMetaInfo.setAcknowledgement(acknowledgement);
			}

		}
		return collection;
	}
	
	private void setView(Resource resource) {
		try {
			String  view = this.resourceCassandraService.get(resource.getGooruOid(), STATISTICS_VIEW_COUNT);
			resource.setViews(view == null ? 0L : Long.parseLong(view));
			resource.setViewCount(resource.getViews());
		} catch (Exception e) {
			LOGGER.error(_ERROR, e);
		}
	} 

	private void setCollectionTaxonomyMetaInfo(final Set<Code> taxonomySet, final ResourceMetaInfo collectionMetaInfo) { 
		if (taxonomySet != null) {
			final Set<String> course = new HashSet<String>();
			final Set<Map<String, Object>> skills =  new HashSet<Map<String, Object>>();
			
			for (final Code code : taxonomySet) {
				if (code.getDepth() == 2 && code.getRootNodeId() != null && code.getRootNodeId().toString().equalsIgnoreCase(Code.GOORU_TAXONOMY_CODE_ID)) {
					course.add(code.getLabel());
				} else if (code.getCodeType() != null && code.getCodeType().getLabel() != null && code.getCodeType().getLabel().equalsIgnoreCase(Constants.TWENTY_FIRST_CENTURY_SKILLS)) {
					skills.add(setSkill(code));
				}
			}
			collectionMetaInfo.setSkills(skills);
			collectionMetaInfo.setCourse(course);
		}
	}
	
	private Map<String, Object> setSkill(final Code code) { 
		Map<String, Object> skill = new HashMap<String, Object>();
		skill.put(CODE_ID, code.getCodeId());
		skill.put(LABEL, code.getLabel());
		return skill;
	}
	
	@Override
	public Set<Map<String, Object>> getSkills(final Set<Code> taxonomySet) {
		Set<Map<String, Object>> skills =  null;
		if (taxonomySet != null) {
			skills = new HashSet<Map<String, Object>>();
			for (final Code code : taxonomySet) {
				if (code.getCodeType() != null && code.getCodeType().getLabel() != null && code.getCodeType().getLabel().equalsIgnoreCase(Constants.TWENTY_FIRST_CENTURY_SKILLS)) {
					skills.add(setSkill(code));
				}
			}
		}
		return skills;
	}
	
	@Override
	public List<CollectionItem> setCollectionItemMetaInfo(final List<CollectionItem> collectionItems, final String rootNodeId, final boolean includeView) {
		if (collectionItems != null) {
			for (final CollectionItem collectionItem : collectionItems) {
				if (collectionItem.getResource() != null && collectionItem.getResource().getResourceType().getName().equalsIgnoreCase(ResourceType.Type.SCOLLECTION.getType())) {
					collectionItem.setCourse(this.getCourse(collectionItem.getResource().getTaxonomySet()));
					collectionItem.setStandards(this.getStandards(collectionItem.getResource().getTaxonomySet(), false, rootNodeId));
					List<CollectionItem> collectionItemCount = this.getCollectionItems(collectionItem.getResource().getGooruOid(), new HashMap<String, String>());
					collectionItem.setResourceCount(collectionItemCount.size());
				}
				if (includeView) {
					setView(collectionItem.getResource());
				}
			}
		}
		return collectionItems;
	}

	private CollectionItem setCollectionItemMoreData(final CollectionItem collectionItem, final String rootNodeId) {
		if (collectionItem.getResource() != null) {
			if (collectionItem.getResource().getResourceType().getName().equals(ResourceType.Type.ASSESSMENT_QUESTION.getType())) {
				collectionItem.setQuestionInfo(this.getAssessmentService().getQuestion(collectionItem.getResource().getGooruOid()));
			}
			if (collectionItem.getResource().getResourceType().getName().equals(ResourceType.Type.TEXTBOOK.getType())) {
				final Textbook textbook = this.getResourceRepository().findTextbookByContentGooruId(collectionItem.getResource().getGooruOid());
				if (textbook != null) {
					collectionItem.setDocumentid(textbook.getDocumentId());
					collectionItem.setDocumentkey(textbook.getDocumentKey());
				}
			}

			collectionItem.setStandards(this.getStandards(collectionItem.getResource().getTaxonomySet(), false, rootNodeId));
		}

		return collectionItem;
	}

	@Override
	public Set<String> getCourse(final Set<Code> taxonomySet) {
		Set<String> course = null;
		if (taxonomySet != null) {
			course = new HashSet<String>();
			for (final Code code : taxonomySet) {
				if (code.getDepth() == 2 && code.getRootNodeId() != null && code.getRootNodeId().toString().equalsIgnoreCase(Code.GOORU_TAXONOMY_CODE_ID)) {
					course.add(code.getLabel());
				}
			}
		}
		return course;
	}
	

	@Override
	public List<StandardFo> getStandards(final Set<Code> taxonomySet, final boolean ignoreUserTaxonomyPreference, final String rootNodeId) {
		List<StandardFo> standards = null;
		if (taxonomySet != null) {
			standards = new ArrayList<StandardFo>();
			if (!ignoreUserTaxonomyPreference) {
				final String taxonomyPreference = rootNodeId != null && !rootNodeId.equalsIgnoreCase(Code.GOORU_TAXONOMY_CODE_ID) ? rootNodeId : UserGroupSupport.getTaxonomyPreference();
				for (final Code code : taxonomySet) {
					if (code.getRootNodeId() != null && taxonomyPreference != null && taxonomyPreference.contains(code.getRootNodeId().toString())) {
						standards.add(getStandards(code));
					}
				}
			} else {
				for (final Code code : taxonomySet) {
					if (code.getRootNodeId() != null && !code.getRootNodeId().toString().equalsIgnoreCase(Code.GOORU_TAXONOMY_CODE_ID)) {
						standards.add(getStandards(code));
					}
				}
			}

		}
		return standards;
	}

	private StandardFo getStandards(final Code code) {
		final StandardFo standard = new StandardFo();
		if (code.getCommonCoreDotNotation() != null && !code.getCommonCoreDotNotation().equals("")) {
			standard.setCode(code.getCommonCoreDotNotation().replace(".--", " "));
		} else if (code.getdisplayCode() != null && !code.getdisplayCode().equals("")) {
			standard.setCode(code.getdisplayCode().replace(".--", " "));
		}
		if (code.getLabel() != null && !code.getLabel().equals("")) {
			standard.setDescription(code.getLabel());
		} else {
			standard.setDescription(BLANK + code.getCode());
		}
		standard.setCodeId(code.getCodeId());
		return standard;
	}

	@Override
	public List<Collection> getResourceMoreInfo(final String resourceGooruOid) {
		return this.getCollectionRepository().getCollectionsByResourceId(resourceGooruOid);
	}

	@Override
	public Collection getCollectionByGooruOid(final String gooruOid, final String gooruUid) {
		return getCollectionRepository().getCollectionByGooruOid(gooruOid, gooruUid);
	}

	@Override
	public CollectionItem getCollectionItemById(final String collectionItemId) {
		return getCollectionRepository().getCollectionItemById(collectionItemId);
	}

	@Override
	public Collection updateCollectionMetadata(final String collectionId, final String creatorUId, String ownerUId, final boolean hasUnrestrictedContentAccess, final MultiValueMap<String, String> data, final User apiCallerUser) {
		final Collection collection = this.getCollectionByGooruOid(collectionId, null);
		rejectIfNull(collection, GL0056, _COLLECTION);
		Boolean taxonomyByCode = false;
		final String taxonomyCode = data.getFirst(TAXONOMY_CODE);
		final String title = data.getFirst(TITLE);
		final String description = data.getFirst(DESCRIPTION);
		final String grade = data.getFirst(GRADE);
		final String sharing = data.getFirst(SHARING);
		final String narrationLink = data.getFirst(NARRATION_LINK);
		final String vocabulary = data.getFirst(VOCABULARY);
		final String updateTaxonomyByCode = data.getFirst(UPDATE_TAXONOMY_BY_CODE);
		final String action = data.getFirst(ACTION);
		final String mediaType = data.getFirst(MEDIA_TYPE);
		final String buildType = data.getFirst(BUILD_TYPE);

		if (isNotEmptyString(updateTaxonomyByCode) && updateTaxonomyByCode.equalsIgnoreCase(TRUE)) {
			taxonomyByCode = true;
		}

		if (isNotEmptyString(taxonomyCode)) {
			if (isNotEmptyString(action) && action.equalsIgnoreCase(DELETE)) {
				deleteCollectionTaxonomy(collection, taxonomyCode, taxonomyByCode);
			} else {
				addCollectionTaxonomy(collection, taxonomyCode, taxonomyByCode);
			}
			this.getCollectionRepository().save(collection);
		}
		if (isNotEmptyString(buildType)) {
			if (buildType.equalsIgnoreCase(WEB) || buildType.equalsIgnoreCase(IPAD)) {
				collection.setBuildType(this.getCustomTableRepository().getCustomTableValue(CustomProperties.Table.BUILD_TYPE.getTable(), buildType));
			}
		}
		if (isNotEmptyString(mediaType)) {
			collection.setMediaType(mediaType);
		}
		if (isNotEmptyString(title)) {
			collection.setTitle(title);
		}

		if (description != null) {
			collection.setGoals(description);
		}
		collection.setLastUpdatedUserUid(apiCallerUser.getPartyUid());

		if (isNotEmptyString(sharing)) {
			collection.setSharing(sharing);
			this.getCollectionRepository().save(collection);
			resetFolderVisibility(collection.getGooruOid(), apiCallerUser.getPartyUid());
			updateResourceSharing(sharing, collection);
		}

		if (isNotEmptyString(vocabulary)) {
			collection.setVocabulary(vocabulary);
		}
		if (data.containsKey(GRADE)) {
			saveOrUpdateCollectionGrade(grade, collection, false);
		}
		if (isNotEmptyString(narrationLink)) {
			collection.setNarrationLink(narrationLink);
		}

		if (hasUnrestrictedContentAccess) {
			if (creatorUId != null) {
				User user = getUserService().findByGooruId(creatorUId);
				collection.setCreator(user);
			}
			if (ownerUId != null) {
				final User user = getUserService().findByGooruId(ownerUId);
				collection.setUser(user);
			}
		}
		this.setCollectionMetaData(collection, null, null, true, null, false, false, false);
		this.getCollectionRepository().save(collection);
		try {
			indexHandler.setReIndexRequest(collection.getGooruOid(), IndexProcessor.INDEX, SCOLLECTION, null, false, false);					
		} catch (Exception e) {
			LOGGER.error(_ERROR, e);
		}
		getAsyncExecutor().deleteFromCache(V2_ORGANIZE_DATA + collection.getUser().getPartyUid() + "*");
		try {
			this.getCollectionEventLog().getEventLogs(collection, apiCallerUser, false, true, SerializerUtil.serializeToJson(data, true, true));
		} catch (Exception e) {
			LOGGER.error(_ERROR, e);
		}
		return collection;
	}

	private Boolean isNotEmptyString(final String field) {
		return StringUtils.hasLength(field);
	}

	public void updateResourceSharing(final String sharing, final Collection collection) {
		final Iterator<CollectionItem> iterator = collection.getCollectionItems().iterator();
		while (iterator.hasNext()) {
			final CollectionItem collectionItem = iterator.next();
			if (!collectionItem.getResource().getSharing().equalsIgnoreCase(Sharing.PUBLIC.getSharing())) {
				collectionItem.getResource().setSharing(sharing);
				this.getCollectionRepository().save(collectionItem);
			}
		}
	}

	@Override
	public CollectionItem updateCollectionItemMetadata(final String collectionItemId, final MultiValueMap<String, String> data, final User apiCaller) {

		final CollectionItem collectionItem = this.getCollectionItemById(collectionItemId);

		rejectIfNull(collectionItem, GL0056, _COLLECTION_ITEM);

		ServerValidationUtils.rejectIfMaxLimitExceed(8, data.getFirst(NARRATION_TYPE), "GL0014", NARRATION_TYPE, "8");
		ServerValidationUtils.rejectIfMaxLimitExceed(8, data.getFirst(START), "GL0014", START, "8");
		ServerValidationUtils.rejectIfMaxLimitExceed(8, data.getFirst(STOP), "GL0014", STOP, "8");
		
		final String narration = data.getFirst(NARRATION);
		final String narrationType = data.getFirst(NARRATION_TYPE);
		final String start = data.getFirst(START);
		final String stop = data.getFirst(STOP);

		if (data.containsKey(NARRATION)) {
			collectionItem.setNarration(narration);
		}
		if (isNotEmptyString(narrationType)) {
			collectionItem.setNarrationType(narrationType);
		}
		if (data.containsKey(START)) {
			collectionItem.setStart(start);
		}
		if (data.containsKey(STOP)) {
			collectionItem.setStop(stop);
		}
		if (collectionItem.getResource() != null && collectionItem.getResource().getResourceType() != null && collectionItem.getResource().getResourceType().getName().equalsIgnoreCase(ResourceType.Type.SCOLLECTION.getType())) {
			collectionItem.getResource().setLastUpdatedUserUid(apiCaller.getPartyUid());
		}
		this.getCollectionRepository().save(collectionItem);
		try {
			indexHandler.setReIndexRequest(collectionItem.getResource().getGooruOid(), IndexProcessor.INDEX, RESOURCE, null, false, false);					
			indexHandler.setReIndexRequest(collectionItem.getCollection().getGooruOid(), IndexProcessor.INDEX, SCOLLECTION, null, false, false);					
		} catch (Exception e) {
			LOGGER.error(_ERROR, e);
		}
		getAsyncExecutor().deleteFromCache(V2_ORGANIZE_DATA + collectionItem.getCollection().getUser().getPartyUid() + "*");
	     try {
	    	 
	    	 this.getCollectionEventLog().getEventLogs(collectionItem, SerializerUtil.serializeToJson(data, true, true), apiCaller);
	     } catch (Exception e) {
	    	 LOGGER.error(_ERROR, e);
	     }
		return collectionItem;
	}

	@Override
	public CollectionItem copyCollectionItem(final String collectionItemId, final String collectionId) throws Exception {
		final CollectionItem sourceCollectionItem = this.getCollectionItem(collectionItemId, false, null, null);
		rejectIfNull(sourceCollectionItem, GL0056, _COLLECTION_ITEM);

		final CollectionItem destCollectionItem = new CollectionItem();
		Collection targetCollection = null;
		boolean hasSameCollection = false;
		if (collectionId != null) {
			targetCollection = this.getCollection(collectionId, false, false, false, sourceCollectionItem.getCollection().getUser(), null, null, false, false, false, false);
		}
		if (targetCollection == null) {
			targetCollection = sourceCollectionItem.getCollection();
		}
		if (targetCollection.getGooruOid().equalsIgnoreCase(sourceCollectionItem.getCollection().getGooruOid())) {
			hasSameCollection = true;
		}
		if (sourceCollectionItem.getResource().getResourceType().getName().equalsIgnoreCase(ResourceType.Type.ASSESSMENT_QUESTION.getType())) {
			AssessmentQuestion assessmentQuestion = assessmentService.copyAssessmentQuestion(sourceCollectionItem.getCollection().getUser(), sourceCollectionItem.getResource().getGooruOid());
			destCollectionItem.setResource(assessmentQuestion);
		} else {
			destCollectionItem.setResource(sourceCollectionItem.getResource());
			destCollectionItem.getResource().setCopiedResourceId(sourceCollectionItem.getCollectionItemId());
		}
		destCollectionItem.setItemType(sourceCollectionItem.getItemType());

		final int sequence = targetCollection.getCollectionItems().size() > 0 ? hasSameCollection ? (sourceCollectionItem.getItemSequence() + 1) : (targetCollection.getCollectionItems().size() + 1) : 1;

		destCollectionItem.setCollection(targetCollection);
		destCollectionItem.setItemSequence(sequence);
		destCollectionItem.setNarration(sourceCollectionItem.getNarration());
		destCollectionItem.setNarrationType(sourceCollectionItem.getNarrationType());
		destCollectionItem.setStart(sourceCollectionItem.getStart());
		destCollectionItem.setStop(sourceCollectionItem.getStop());
		destCollectionItem.setAssociatedUser(targetCollection.getUser());
		destCollectionItem.setAssociationDate(new Date(System.currentTimeMillis()));

		this.getCollectionRepository().save(destCollectionItem);
		if (hasSameCollection) {
			resetCollectionItemSequence(sourceCollectionItem.getCollectionItemId(), targetCollection);
		}
		this.getCollectionEventLog().getEventLogs(destCollectionItem, true, false, destCollectionItem.getCollection() != null && destCollectionItem.getCollection().getUser() != null ? destCollectionItem.getCollection().getUser() : null, true, false, sourceCollectionItem, null);
		
		return destCollectionItem;
	}

	private void resetCollectionItemSequence(final String collectionItemId, final Collection collection) {
		int itemSequence = 1;
		int count = 1;
		for (final CollectionItem item : collection.getCollectionItems()) {
			if (item.getCollectionItemId().equals(collectionItemId)) {
				itemSequence = item.getItemSequence() + 1;
				count++;
			}else if(count>1){
				item.setItemSequence(++itemSequence);
			}
		}
		this.getCollectionRepository().save(collection);
	}

	@Override
	public ActionResponseDTO<CollectionItem> createResourceWithCollectionItem(final String collectionId, final String title, final String description, final String url, final String start, final String stop, final String thumbnailImgSrc, final String resourceType, final String category, final User user) throws Exception {
		final Resource newResource = new Resource();
		newResource.setTitle(title);
		newResource.setDescription(description);
		newResource.setUrl(url);
		newResource.setThumbnail(thumbnailImgSrc);
		newResource.setCategory(category);
		return this.createResourceWithCollectionItem(collectionId, newResource, start, stop, null, user);
	}

	@Override
	public CollectionItem buildCollectionItemFromInputParameters(final String data, final User user) {
		final XStream xstream = new XStream(new JettisonMappedXmlDriver());
		xstream.alias(COLLECTION_ITEM, CollectionItem.class);
		return (CollectionItem) xstream.fromXML(data);
	}

	@Override
	public List<Collection> getMyCollection(final Map<String, String> filters, final User user) {
		return getCollectionRepository().getMyCollection(filters, user);
	}

	@Override
	public List<Collection> getMyCollection(final String offset, final String limit, final String type, final String filter, final User user) {
		return this.getCollectionRepository().getMyCollection(offset, limit, type, filter, user);
	}

	protected void reOrderCollectionItems(final Collection collection, final String collectionItemId) {
		int resetSequence = 1;
		final Set<CollectionItem> items = collection.getCollectionItems();
		for (final CollectionItem item : items) {
			if (item.getCollectionItemId().equals(collectionItemId)) {
				items.remove(item);
				break;
			}
		}
		for (CollectionItem item : items) {
			item.setItemSequence(resetSequence++);
		}
		this.getCollectionRepository().saveAll(items);
	}

	private void deleteCollectionTaxonomy(final Collection collection, final String taxonomyCode, final boolean updateTaxonomyByCode) {
		final String[] taxonomyCodes = taxonomyCode.split(",");
		final Set<Code> codes = collection.getTaxonomySet();
		final Set<Code> removeCodes = new HashSet<Code>();
		for (final String codeId : taxonomyCodes) {
			Code removeCode = null;
			if (updateTaxonomyByCode) {
				removeCode = (Code) this.getTaxonomyRepository().findCodeByTaxCode(codeId);
			} else {
				removeCode = (Code) this.getTaxonomyRepository().findCodeByCodeId(Integer.parseInt(codeId));
			}
			if (removeCode != null) {
				for (final Code code : codes) {
					if (code.getCodeId().equals(removeCode.getCodeId())) {
						removeCodes.add(removeCode);
					}
				}

			}
			if (removeCodes.size() > _ZERO) {
				codes.removeAll(removeCodes);
			}
		}
		collection.setTaxonomySet(codes);
	}

	private Errors validateCollection(final Collection collection) throws Exception {
		final Errors errors = new BindException(collection, COLLECTION);
		if (collection != null) {
			rejectIfNullOrEmpty(errors, collection.getTitle(), TITLE, GL0006, generateErrorMessage(GL0006, TITLE));
			rejectIfInvalidType(errors, collection.getCollectionType(), COLLECTION_TYPE, GL0007, generateErrorMessage(GL0007, COLLECTION_TYPE), Constants.COLLECTION_TYPES);
		}
		return errors;
	}

	private Errors validateUpdateCollection(final Collection collection) throws Exception {
		final Errors errors = new BindException(collection, COLLECTION);
		rejectIfNull(errors, collection, COLLECTION, GL0006, generateErrorMessage(GL0006, COLLECTION));
		return errors;
	}

	private Errors validateCollectionItem(final Collection collection, final Resource resource, final CollectionItem collectionItem) throws Exception {
		final Map<String, String> itemType = new HashMap<String, String>();
		itemType.put(ADDED, COLLECTION_ITEM_TYPE);
		itemType.put(SUBSCRIBED, COLLECTION_ITEM_TYPE);
		itemType.put(COLLABORATOR, COLLECTION_ITEM_TYPE);
		itemType.put("class", COLLECTION_ITEM_TYPE);
		final Errors errors = new BindException(collectionItem, COLLECTION_ITEM);
		if (collectionItem != null) {
			rejectIfNull(errors, collection, COLLECTION, GL0056, generateErrorMessage(GL0056, COLLECTION));
			rejectIfNull(errors, resource, RESOURCE, GL0056, generateErrorMessage(GL0056, RESOURCE));
			rejectIfInvalidType(errors, collectionItem.getItemType(), ITEM_TYPE, GL0007, generateErrorMessage(GL0007, ITEM_TYPE), itemType);
		}
		return errors;
	}

	private Errors validateUpdateCollectionItem(final CollectionItem collectionItem) throws Exception {
		final Errors errors = new BindException(collectionItem, COLLECTION_ITEM);
		rejectIfNull(errors, collectionItem, COLLECTION_ITEM, "GL0056", generateErrorMessage(GL0056, COLLECTION_ITEM));
		return errors;
	}

	private Errors validateReorderCollectionItem(final CollectionItem collectionItem) throws Exception {
		final Errors errors = new BindException(collectionItem, COLLECTION_ITEM);
		if (collectionItem != null) {
			rejectIfNull(errors, collectionItem, COLLECTION_ITEM, GL0056, generateErrorMessage(GL0056, COLLECTION_ITEM));
		}
		return errors;
	}

	@Override
	public List<CollectionItem> getCollectionItemByResourceId(final Long resourceId) {
		return collectionRepository.getCollectionItemByResourceId(resourceId);

	}

	@Override
	public ActionResponseDTO<Collection> updateCollection(final Collection newCollection, final String updateCollectionId, final String ownerUId, final String creatorUId, final boolean hasUnrestrictedContentAccess, final String relatedContentId, final User updateUser, final String data) throws Exception {
		String gooruUid = null;
		if (newCollection.getUser() != null && !userService.isContentAdmin(updateUser)) {
			gooruUid = newCollection.getUser().getGooruUId();
		}
		final Collection collection = this.getCollectionByGooruOid(updateCollectionId, gooruUid);
		rejectIfNull(collection, GL0056, _COLLECTION);
		final Errors errors = validateUpdateCollection(collection);
		if (!errors.hasErrors()) {

			if (relatedContentId != null) {
				final Content assocContent = this.getContentRepositoryHibernate().findContentByGooruId(relatedContentId);

				if (assocContent != null && collection != null) {
					final ContentAssociation contentAssoc = new ContentAssociation();
					contentAssoc.setAssociateContent(assocContent);
					contentAssoc.setContent(collection);
					contentAssoc.setModifiedDate(new Date());
					contentAssoc.setUser(collection.getUser());
					contentAssoc.setTypeOf(RELATED_CONTENT);
					this.getContentRepositoryHibernate().save(contentAssoc);
					collection.setContentAssociation(contentAssoc);
				}

			}

			if (newCollection.getTaxonomySet() != null) {
				resourceService.saveOrUpdateResourceTaxonomy(collection, newCollection.getTaxonomySet());
			}

			if (newCollection.getVocabulary() != null) {
				collection.setVocabulary(newCollection.getVocabulary());
			}
			if (newCollection.getBuildType() != null && newCollection.getBuildType().getValue() != null) {
				if (newCollection.getBuildType().getValue().equalsIgnoreCase(WEB) || newCollection.getBuildType().getValue().equalsIgnoreCase(IPAD)) {
					collection.setBuildType(this.getCustomTableRepository().getCustomTableValue(CustomProperties.Table.BUILD_TYPE.getTable(), newCollection.getBuildType().getValue()));
				}
			}
			if (newCollection.getPublishStatus() != null && newCollection.getPublishStatus().getValue() != null) {
				if (newCollection.getPublishStatus().getValue().equalsIgnoreCase(REVIEWED)) {
					collection.setPublishStatus(this.getCustomTableRepository().getCustomTableValue(_PUBLISH_STATUS, newCollection.getPublishStatus().getValue()));
				}
			}
			if (newCollection.getMediaType() != null) {
				collection.setMediaType(newCollection.getMediaType());
			}
			if (newCollection.getTitle() != null) {
				collection.setTitle(newCollection.getTitle());
			}

			if (newCollection.getMailNotification() != null) {
				collection.setMailNotification(newCollection.getMailNotification());
			}
			if (newCollection.getDescription() != null) {
				collection.setDescription(newCollection.getDescription());
			}
			if (newCollection.getNarrationLink() != null) {
				collection.setNarrationLink(newCollection.getNarrationLink());
			}
			if (newCollection.getEstimatedTime() != null) {
				collection.setEstimatedTime(newCollection.getEstimatedTime());
			}
			if (newCollection.getNotes() != null) {
				collection.setNotes(newCollection.getNotes());
			}
			
			if (newCollection.getUrl() != null) {
				collection.setUrl(newCollection.getUrl());
			}
			
			if (newCollection.getGoals() != null) {
				collection.setGoals(newCollection.getGoals());
			}
			if (newCollection.getKeyPoints() != null) {
				collection.setKeyPoints(newCollection.getKeyPoints());
			}
			if (newCollection.getLanguage() != null) {
				collection.setLanguage(newCollection.getLanguage());
			}
			if (newCollection.getGrade() != null) {
				collection.setGrade(newCollection.getGrade());
			}
			if (newCollection.getLanguageObjective() != null) {
				collection.setLanguageObjective(newCollection.getLanguageObjective());
			}
			if (newCollection.getIdeas() != null) {
				collection.setIdeas(newCollection.getIdeas());
			}
			if (newCollection.getQuestions() != null) {
				collection.setQuestions(newCollection.getQuestions());
			}
			if (newCollection.getPerformanceTasks() != null) {
				collection.setPerformanceTasks(newCollection.getPerformanceTasks());
			}

			if (newCollection.getSettings() != null) {
				ContentSettings contentSettings = null;
				final Map<String, String> settings = new HashMap<String, String>();
				if (collection.getContentSettings() != null && collection.getContentSettings().size() > 0) {
					contentSettings = collection.getContentSettings().iterator().next();
					final Map<String, String> contentSettingsMap = JsonDeserializer.deserialize(contentSettings.getData(), new TypeReference<Map<String, String>>() {
					});
					settings.putAll(contentSettingsMap);
				}
				settings.putAll(newCollection.getSettings());
				newCollection.setSettings(settings);
				ContentSettings contentSetting = contentSettings == null ? new ContentSettings() : contentSettings;
				contentSetting.setContent(collection);
				contentSetting.setData(new JSONSerializer().exclude("*.class").serialize(newCollection.getSettings()));
				this.getCollectionRepository().save(contentSetting);
			}

			if (collection.getResourceType().getName().equalsIgnoreCase(SCOLLECTION)) {
				if (newCollection.getDepthOfKnowledges() != null && newCollection.getDepthOfKnowledges().size() > 0) {
					collection.setDepthOfKnowledges(this.updateContentMeta(newCollection.getDepthOfKnowledges(), collection, updateUser, DEPTH_OF_KNOWLEDGE));
				} else {
					collection.setDepthOfKnowledges(this.setContentMetaAssociation(this.getContentMetaAssociation(DEPTH_OF_KNOWLEDGE), collection, DEPTH_OF_KNOWLEDGE));
				}

				if (newCollection.getLearningSkills() != null && newCollection.getLearningSkills().size() > 0) {
					collection.setLearningSkills(this.updateContentMeta(newCollection.getLearningSkills(), collection, updateUser, LEARNING_AND_INNOVATION_SKILLS));
				} else {
					collection.setLearningSkills(this.setContentMetaAssociation(this.getContentMetaAssociation(LEARNING_AND_INNOVATION_SKILLS), collection, LEARNING_AND_INNOVATION_SKILLS));
				}

				if (newCollection.getAudience() != null && newCollection.getAudience().size() > 0) {
					collection.setAudience(this.updateContentMeta(newCollection.getAudience(), collection, updateUser, AUDIENCE));
				} else {
					collection.setAudience(this.setContentMetaAssociation(this.getContentMetaAssociation(AUDIENCE), collection, AUDIENCE));
				}

				if (newCollection.getInstructionalMethod() != null && newCollection.getInstructionalMethod().size() > 0) {
					collection.setInstructionalMethod(this.updateContentMeta(newCollection.getInstructionalMethod(), collection, updateUser, INSTRUCTIONAL_METHOD));
				} else {
					collection.setInstructionalMethod(this.setContentMetaAssociation(this.getContentMetaAssociation(INSTRUCTIONAL_METHOD), collection, INSTRUCTIONAL_METHOD));
				}
			}
			if (newCollection.getSharing() != null && (newCollection.getSharing().equalsIgnoreCase(Sharing.PRIVATE.getSharing()) || newCollection.getSharing().equalsIgnoreCase(Sharing.PUBLIC.getSharing()) || newCollection.getSharing().equalsIgnoreCase(Sharing.ANYONEWITHLINK.getSharing()))) {

				if (!newCollection.getSharing().equalsIgnoreCase(PUBLIC)) {
					collection.setPublishStatus(null);
				}
				if (!collection.getCollectionType().equalsIgnoreCase(ResourceType.Type.ASSESSMENT_URL.getType()) && newCollection.getSharing().equalsIgnoreCase(PUBLIC) && !userService.isContentAdmin(updateUser)) {
					collection.setPublishStatus(this.getCustomTableRepository().getCustomTableValue(_PUBLISH_STATUS, PENDING));
					newCollection.setSharing(collection.getSharing());
				}
				if (collection.getCollectionType().equalsIgnoreCase(ResourceType.Type.ASSESSMENT_URL.getType()) || newCollection.getSharing().equalsIgnoreCase(PUBLIC) && userService.isContentAdmin(updateUser)) {
					collection.setPublishStatus(this.getCustomTableRepository().getCustomTableValue(_PUBLISH_STATUS, REVIEWED));
				}
				if (collection.getSharing().equalsIgnoreCase(PUBLIC) && (newCollection.getSharing().equalsIgnoreCase(Sharing.PRIVATE.getSharing()) || newCollection.getSharing().equalsIgnoreCase(Sharing.ANYONEWITHLINK.getSharing()))) {
					final UserSummary userSummary = this.getUserRepository().getSummaryByUid(collection.getUser().getPartyUid());
					if (userSummary.getCollections() == null || userSummary.getCollections() == 0) {
						PartyCustomField partyCustomField = new PartyCustomField(USER_META, SHOW_PROFILE_PAGE, TRUE);
						this.getPartyService().updatePartyCustomField(collection.getUser().getPartyUid(), partyCustomField, collection.getUser());
					}
					if (userSummary.getGooruUid() != null) {
						userSummary.setCollections(userSummary.getCollections() <= 0 ? 0 : (userSummary.getCollections() - 1));
						this.getUserRepository().save(userSummary);
					}
				} else if ((collection.getCollectionType().equalsIgnoreCase(ResourceType.Type.ASSESSMENT_URL.getType()) || hasUnrestrictedContentAccess) && !collection.getSharing().equalsIgnoreCase(PUBLIC) && newCollection.getSharing().equalsIgnoreCase(PUBLIC)) {
					UserSummary userSummary = this.getUserRepository().getSummaryByUid(collection.getUser().getPartyUid());
					if (userSummary.getGooruUid() == null) {
						userSummary.setGooruUid(collection.getUser().getPartyUid());
					}
					userSummary.setCollections((userSummary.getCollections() != null ? userSummary.getCollections() : 0) + 1);
					this.getUserRepository().save(userSummary);
				}
				if (newCollection.getSharing().equalsIgnoreCase(PRIVATE)) {
					List<CollectionItem> associations = this.getCollectionRepository().getCollectionItemByAssociation(collection.getGooruOid(), null, CLASSPAGE);
					for (CollectionItem item : associations) {
						this.deleteCollectionItem(item.getCollectionItemId(), updateUser, true);
					}
				}
				collection.setSharing(newCollection.getSharing());

				updateResourceSharing(newCollection.getSharing(), collection);
				resetFolderVisibility(collection.getGooruOid(), collection.getUser().getPartyUid());
			}

			collection.setLastUpdatedUserUid(updateUser.getPartyUid());

			if (hasUnrestrictedContentAccess) {
				if (creatorUId != null) {
					User user = getUserService().findByGooruId(creatorUId);
					collection.setCreator(user);
				}
				if (ownerUId != null) {
					final User user = getUserService().findByGooruId(ownerUId);
					collection.setUser(user);
				}
				if (newCollection.getNetwork() != null) {
					collection.setNetwork(newCollection.getNetwork());
				}
			}

			this.getCollectionRepository().save(collection);

			try {
				if(!collection.getCollectionType().equalsIgnoreCase(ResourceType.Type.ASSESSMENT_URL.getType())){
					indexHandler.setReIndexRequest(collection.getGooruOid(), IndexProcessor.INDEX, SCOLLECTION, null, false, false);
				}
			} catch (Exception e) {
				LOGGER.error(_ERROR, e);
			}
			getAsyncExecutor().deleteFromCache(V2_ORGANIZE_DATA + collection.getUser().getPartyUid() + "*");
		}
		this.getCollectionEventLog().getEventLogs(collection, collection.getUser(), false, true, data);
		return new ActionResponseDTO<Collection>(collection, errors);
	}

	@Override
	public CollectionItem getCollectionItem(final String collectionItemId, final String includeAdditionalInfo, final User user, final String rootNodeId) {

		CollectionItem collectionItem = this.getCollectionRepository().getCollectionItemById(collectionItemId);
		rejectIfNull(collectionItem, GL0056, _COLLECTION_ITEM);
		if (includeAdditionalInfo.equalsIgnoreCase(TRUE)) {
			collectionItem = this.setCollectionItemMoreData(collectionItem, rootNodeId);
		}
		return collectionItem;
	}

	@Override
	public Collection copyCollection(final String collectionId, final Collection newCollection, final boolean addToShelf, final String parentId, final User user) throws Exception {
		final Collection sourceCollection = this.getCollection(collectionId, false, false, false, user, null, null, false, false, false, false);
		rejectIfNull(sourceCollection, GL0056, _COLLECTION);
		CollectionItem collectionItem = null;
		Collection destCollection = null;
		if (sourceCollection != null) {
			destCollection = new Collection();
			if (newCollection.getTitle() != null) {
				destCollection.setTitle(newCollection.getTitle());
			} else {
				destCollection.setTitle(sourceCollection.getTitle());
			}
			destCollection.setGoals(sourceCollection.getGoals());
			destCollection.setCopiedResourceId(sourceCollection.getGooruOid());
			destCollection.setCollectionType(sourceCollection.getCollectionType());
			destCollection.setResourceFormat(sourceCollection.getResourceFormat());
			destCollection.setDescription(sourceCollection.getDescription());
			destCollection.setNotes(sourceCollection.getNotes());
			destCollection.setLanguage(sourceCollection.getLanguage());
			destCollection.setThumbnail(sourceCollection.getThumbnail());
			if (newCollection.getGrade() != null) {
				destCollection.setGrade(newCollection.getGrade());
			}
			destCollection.setEstimatedTime(sourceCollection.getEstimatedTime());
			destCollection.setNarrationLink(sourceCollection.getNarrationLink());
			destCollection.setGooruOid(UUID.randomUUID().toString());
			destCollection.setContentType(sourceCollection.getContentType());
			destCollection.setResourceType(sourceCollection.getResourceType());
			destCollection.setLastModified(new Date(System.currentTimeMillis()));
			destCollection.setCreatedOn(new Date(System.currentTimeMillis()));
	
			if (newCollection != null && newCollection.getSharing() != null) {
				destCollection.setSharing(newCollection.getSharing());
			} else {
			      destCollection.setSharing(sourceCollection.getSharing().equalsIgnoreCase(Sharing.PUBLIC.getSharing()) ? Sharing.ANYONEWITHLINK.getSharing() : sourceCollection.getSharing());
			}
			destCollection.setUser(user);
			destCollection.setOrganization(sourceCollection.getOrganization());
			destCollection.setCreator(sourceCollection.getCreator());
			destCollection.setDistinguish(sourceCollection.getDistinguish());
			destCollection.setIsFeatured(sourceCollection.getIsFeatured());
			this.getCollectionRepository().save(destCollection);
			if (newCollection.getTaxonomySet() != null && newCollection.getTaxonomySet().size() > 0) {
				resourceService.saveOrUpdateResourceTaxonomy(destCollection, new HashSet<Code>(newCollection.getTaxonomySet()));
			}
			this.getCollectionRepository().save(destCollection);
			final Iterator<CollectionItem> sourceItemIterator = sourceCollection.getCollectionItems().iterator();
			final Set<CollectionItem> collectionItems = new TreeSet<CollectionItem>();
			while (sourceItemIterator.hasNext()) {
				final CollectionItem sourceItem = sourceItemIterator.next();
				final CollectionItem destItem = new CollectionItem();
				if (sourceItem.getResource().getResourceType().getName().equalsIgnoreCase(ResourceType.Type.ASSESSMENT_QUESTION.getType())) {
					final AssessmentQuestion assessmentQuestion = assessmentService.copyAssessmentQuestion(user, sourceItem.getResource().getGooruOid());
					destItem.setResource(assessmentQuestion);
				} else {
					destItem.setResource(sourceItem.getResource());
				}
				destItem.getResource().setCopiedResourceId(sourceItem.getCollectionItemId());
				destItem.setItemType(sourceItem.getItemType());
				destItem.setItemSequence(sourceItem.getItemSequence());
				destItem.setNarration(sourceItem.getNarration());
				destItem.setNarrationType(sourceItem.getNarrationType());
				destItem.setStart(sourceItem.getStart());
				destItem.setAssociatedUser(user);
				destItem.setStop(sourceItem.getStop());
				destItem.setCollection(destCollection);
				this.getCollectionRepository().save(destItem);
				collectionItems.add(destItem);
			}
			destCollection.setCollectionItems(collectionItems);
			this.getCollectionRepository().save(destCollection);
			getAsyncExecutor().copyResourceFolder(sourceCollection, destCollection);
			if (addToShelf) {
				collectionItem = new CollectionItem();
				collectionItem.setItemType(ShelfType.AddedType.SUBSCRIBED.getAddedType());
				final Collection myCollection = createMyShelfCollection(null, user, CollectionType.SHElf.getCollectionType(), collectionItem);
				collectionItem.setCollection(myCollection);
				collectionItem.setResource(destCollection);
				final int sequence = myCollection.getCollectionItems() != null ? myCollection.getCollectionItems().size() + 1 : 1;
				collectionItem.setItemSequence(sequence);
				collectionItem.setAssociationDate(new Date(System.currentTimeMillis()));
				this.getCollectionRepository().save(collectionItem);
			}

		}
		if (parentId != null) {
			final Collection parentCollection = collectionRepository.getCollectionByGooruOid(parentId, user.getPartyUid());
			if (parentCollection != null) {
				if (!destCollection.getSharing().equalsIgnoreCase(Sharing.PRIVATE.getSharing()) && parentCollection.getCollectionType().equalsIgnoreCase(CollectionType.FOLDER.getCollectionType())) { 
					parentCollection.setSharing(destCollection.getSharing());
					this.getCollectionRepository().save(parentCollection);
				}
				collectionItem = this.createCollectionItem(destCollection.getGooruOid(), parentCollection.getGooruOid(), new CollectionItem(), destCollection.getUser(), CollectionType.FOLDER.getCollectionType(), false).getModel();
				destCollection.setCollectionItem(collectionItem);
			}
		}
		destCollection.setClusterUid(sourceCollection.getGooruOid());
		destCollection.setIsRepresentative(0);
		this.getCollectionRepository().save(destCollection);
		getAsyncExecutor().deleteFromCache(V2_ORGANIZE_DATA + destCollection.getUser().getPartyUid() + "*");
	
			if (destCollection != null) {
				this.getCollectionEventLog().getEventLogs(collectionItem, true, false, user, true, false, null);
			}
		return destCollection;
	}

	@Override
	public ActionResponseDTO<CollectionItem> createResourceWithCollectionItem(String collectionId, Resource newResource, String start, String stop, List<String> tags, User user) throws Exception {

		ActionResponseDTO<CollectionItem> response = null;
		ResourceSource resourceSource = null;
		String domainName = null;
		if (collectionId != null) {
			if (newResource.getUrl() != null && getResourceService().shortenedUrlResourceCheck(newResource.getUrl())) {
				throw new Exception(generateErrorMessage("GL0011"));
			}
			final Collection collection = this.getCollectionRepository().getCollectionByGooruOid(collectionId, null);
			if (collection != null) {
				Resource resource = null;
				if (newResource.getUrl() != null && !newResource.getUrl().isEmpty() && newResource.getAttach() == null) {
					resource = this.getResourceRepository().findResourceByUrl(newResource.getUrl(), Sharing.PUBLIC.getSharing(), null);
				}
				if (resource != null && resource.getSharing() != null && resource.getSharing().equalsIgnoreCase(Sharing.PUBLIC.getSharing())) {
					throw new AccessDeniedException(generateErrorMessage("GL0012"));
				}

				final String sharing = collection.getSharing();
				final String title = newResource.getTitle().length() > 1000 ? newResource.getTitle().substring(0, 1000) : newResource.getTitle();
				if (resource == null) {
					resource = new Resource();
					resource.setGooruOid(UUID.randomUUID().toString());
					resource.setUser(user);
					resource.setTitle(title);
					if (newResource.getCategory() != null) {
						resource.setCategory(newResource.getCategory().toLowerCase());
					}
					if (newResource.getInstructional() != null) {
						final CustomTableValue resourceCategory = this.getCustomTableRepository().getCustomTableValue(RESOURCE_INSTRUCTIONAL_USE, newResource.getInstructional().getValue());
						resource.setInstructional(resourceCategory);
					}
					if (newResource.getResourceFormat() != null) {
						CustomTableValue resourcetype = this.getCustomTableRepository().getCustomTableValue(RESOURCE_CATEGORY_FORMAT, newResource.getResourceFormat().getValue());
						resource.setResourceFormat(resourcetype);
					}
					resource.setDescription(newResource.getDescription());
					License license = new License();
					license.setName(OTHER);
					resource.setRecordSource(Resource.RecordSource.COLLECTION.getRecordSource());
					final ResourceType resourceTypeDo = new ResourceType();
					resource.setResourceType(resourceTypeDo);
					String fileExtension = null;
					if (newResource.getAttach() != null && newResource.getAttach().getFilename() != null) {
						fileExtension = org.apache.commons.lang.StringUtils.substringAfterLast(newResource.getAttach().getFilename(), ".");
						if (fileExtension.contains(PDF) || BaseUtil.supportedDocument().containsKey(fileExtension)) {
							resourceTypeDo.setName(ResourceType.Type.HANDOUTS.getType());
						} else {
							resourceTypeDo.setName(ResourceType.Type.IMAGE.getType());
						}
						resource.setUrl(newResource.getAttach().getFilename());
						resource.setIsOer(1);
						license.setName(CREATIVE_COMMONS);
					} else {
						resource.setUrl(newResource.getUrl());
						if (ResourceImageUtil.getYoutubeVideoId(newResource.getUrl()) != null) {
							resourceTypeDo.setName(ResourceType.Type.VIDEO.getType());
						} else if (newResource.getUrl() != null && newResource.getUrl().contains("vimeo.com")) {
							final String id = org.apache.commons.lang.StringUtils.substringAfterLast(newResource.getUrl(), "/");
							if (org.apache.commons.lang.StringUtils.isNumeric(id)) {
								final ResourceMetadataCo resourceMetadataCo = ResourceImageUtil.getMetaDataFromVimeoVideo(newResource.getUrl());
								resourceTypeDo.setName(ResourceType.Type.VIMEO_VIDEO.getType());
								newResource.setThumbnail(resourceMetadataCo != null ? resourceMetadataCo.getThumbnail() : null);
							} else {
								resourceTypeDo.setName(ResourceType.Type.RESOURCE.getType());
							}

						} else {
							resourceTypeDo.setName(ResourceType.Type.RESOURCE.getType());
						}

					}
					resource.setLicense(license);
					resource.setSharing(sharing);
					domainName = BaseUtil.getDomainName(newResource.getUrl());
					resourceSource = this.getResourceRepository().findResourceSource(domainName);
					if (resourceSource != null && resourceSource.getFrameBreaker() != null && resourceSource.getFrameBreaker() == 1) {
						resource.setHasFrameBreaker(true);
					} else if ((newResource.getUrl() != null && newResource.getUrl().contains(YOUTUBE_URL) && ResourceImageUtil.getYoutubeVideoId(newResource.getUrl()) == null)) {
						resource.setHasFrameBreaker(true);
					} else {
						resource.setHasFrameBreaker(false);
					}

					this.getResourceService().saveOrUpdate(resource);
					resourceService.saveOrUpdateResourceTaxonomy(resource, newResource.getTaxonomySet());
					this.getResourceService().updateYoutubeResourceFeeds(resource, false);
					this.getResourceService().saveOrUpdate(resource);
					this.getResourceService().mapSourceToResource(resource);

					if (newResource.getMomentsOfLearning() != null && newResource.getMomentsOfLearning().size() > 0) {
						resource.setMomentsOfLearning(this.updateContentMeta(newResource.getMomentsOfLearning(), resource, user, MOMENTS_OF_LEARNING));
					} else {
						resource.setMomentsOfLearning(this.setContentMetaAssociation(this.getContentMetaAssociation(MOMENTS_OF_LEARNING), resource, MOMENTS_OF_LEARNING));
					}
					if (newResource.getEducationalUse() != null && newResource.getEducationalUse().size() > 0) {
						resource.setEducationalUse(this.updateContentMeta(newResource.getEducationalUse(), resource, user, EDUCATIONAL_USE));
					} else {
						resource.setEducationalUse(this.setContentMetaAssociation(this.getContentMetaAssociation(EDUCATIONAL_USE), resource, EDUCATIONAL_USE));
					}
					if (newResource.getHost() != null && newResource.getHost().size() > 0) {
						resource.setHost(this.getResourceService().updateContentProvider(resource.getGooruOid(), newResource.getHost(), user, "host"));
					}
					if (tags != null && tags.size() > 0) {
						resource.setResourceTags(this.getContentService().createTagAssoc(resource.getGooruOid(), tags, user));
					}
					try {
						if (newResource.getThumbnail() != null || fileExtension != null && fileExtension.contains(PDF)) {
							this.getResourceImageUtil().downloadAndSendMsgToGenerateThumbnails(resource, newResource.getThumbnail());
						}
					} catch (Exception e) {
						LOGGER.error(_ERROR, e);
					}

					if (resource != null && resource.getContentId() != null) {
						try {
							indexHandler.setReIndexRequest(resource.getGooruOid(), IndexProcessor.INDEX, RESOURCE, null, false, false);									
						} catch (Exception e) {
							LOGGER.error(_ERROR, e);
						}
					}

				}

				if (newResource.getAttach() != null) {
					this.getResourceImageUtil().moveAttachment(newResource, resource);
				}
				collection.setLastUpdatedUserUid(user.getPartyUid());
				this.getResourceRepository().save(collection);

				response = createCollectionItem(resource, collection, start, stop, user);

				response.getModel().setStandards(this.getStandards(resource.getTaxonomySet(), false, null));
				response.getModel().getResource().setSkills(getSkills(resource.getTaxonomySet()));

			} else {
				throw new NotFoundException(generateErrorMessage("GL0013"), "GL0013");
			}
			if (response.getModel().getCollection().getResourceType().getName().equalsIgnoreCase(SCOLLECTION) && response.getModel().getCollection().getClusterUid() != null && !response.getModel().getCollection().getClusterUid().equalsIgnoreCase(response.getModel().getCollection().getGooruOid())) {
				response.getModel().getCollection().setClusterUid(response.getModel().getCollection().getGooruOid());
				this.getCollectionRepository().save(response.getModel().getCollection());
			}
			getAsyncExecutor().deleteFromCache(V2_ORGANIZE_DATA + response.getModel().getCollection().getUser().getPartyUid() + "*");
		}
		this.getCollectionEventLog().getEventLogs(response.getModel(), true, false, user, false, false, null);
		return response;
	}

	@Override
	public ActionResponseDTO<CollectionItem> updateResourceWithCollectionItem(final String collectionItemId, final Resource newResource, final List<String> tags, final User user, String data) throws Exception {
		final CollectionItem collectionItem = this.getCollectionItemById(collectionItemId);
		rejectIfNull(collectionItem, GL0056, 404, COLLECTION_ITEM);
		final Errors errors = validateUpdateCollectionItem(collectionItem);
		if (!errors.hasErrors()) {

			Resource resource = null;

			if (collectionItem != null && collectionItem.getResource() != null) {
				resource = this.getResourceService().findResourceByContentGooruId(collectionItem.getResource().getGooruOid());
			}
			rejectIfNull(resource, GL0056, RESOURCE);
			if (newResource.getTitle() != null) {
				resource.setTitle(newResource.getTitle());
			}
			if (newResource.getDescription() != null) {
				resource.setDescription(newResource.getDescription());
			}
			if (newResource.getCategory() != null) {
				resource.setCategory(newResource.getCategory().toLowerCase());
			}
			if (newResource.getInstructional() != null) {
				final CustomTableValue resourceCategory = this.getCustomTableRepository().getCustomTableValue(RESOURCE_INSTRUCTIONAL_USE, newResource.getInstructional().getValue());
				resource.setInstructional(resourceCategory);
			}
			if (newResource.getResourceFormat() != null) {
				final CustomTableValue resourcetype = this.getCustomTableRepository().getCustomTableValue(RESOURCE_CATEGORY_FORMAT, newResource.getResourceFormat().getValue());
				resource.setResourceFormat(resourcetype);
			}

			if (newResource.getMediaType() != null) {
				resource.setMediaType(newResource.getMediaType());
			}

			if (newResource.getSharing() != null) {
				resource.setSharing(newResource.getSharing());
			}

			String fileExtension = null;
			if (newResource.getAttach() != null && newResource.getAttach().getFilename() != null) {
				fileExtension = org.apache.commons.lang.StringUtils.substringAfterLast(newResource.getAttach().getFilename(), ".");
				final ResourceType resourceTypeDo = new ResourceType();
				resource.setResourceType(resourceTypeDo);
				if (fileExtension.contains(PDF)) {
					resourceTypeDo.setName(ResourceType.Type.HANDOUTS.getType());
				} else {
					resourceTypeDo.setName(ResourceType.Type.IMAGE.getType());
				}
				resource.setUrl(newResource.getAttach().getFilename());
			}
			if (newResource.getS3UploadFlag() != null) {
				resource.setS3UploadFlag(newResource.getS3UploadFlag());
			}

			this.getResourceService().saveOrUpdate(resource);

			resourceService.saveOrUpdateResourceTaxonomy(resource, newResource.getTaxonomySet());

			if (newResource.getMomentsOfLearning() != null && newResource.getMomentsOfLearning().size() > 0) {
				resource.setMomentsOfLearning(this.updateContentMeta(newResource.getMomentsOfLearning(), resource.getGooruOid(), user, MOMENTS_OF_LEARNING));
			} else {
				resource.setMomentsOfLearning(this.setContentMetaAssociation(this.getContentMetaAssociation(MOMENTS_OF_LEARNING), resource, MOMENTS_OF_LEARNING));
			}
			if (newResource.getEducationalUse() != null && newResource.getEducationalUse().size() > 0) {
				resource.setEducationalUse(this.updateContentMeta(newResource.getEducationalUse(), resource.getGooruOid(), user, EDUCATIONAL_USE));
			} else {
				resource.setEducationalUse(this.setContentMetaAssociation(this.getContentMetaAssociation(EDUCATIONAL_USE), resource, EDUCATIONAL_USE));
			}

			if (tags != null && tags.size() > 0) {
				resource.setResourceTags(this.getContentService().createTagAssoc(resource.getGooruOid(), tags, user));
			}

			this.getResourceService().saveOrUpdate(resource);

			if (newResource.getThumbnail() != null && newResource.getThumbnail().length() > 0) {
				try {
					this.getResourceImageUtil().downloadAndSendMsgToGenerateThumbnails(resource, newResource.getThumbnail());
				} catch (Exception e) {
					LOGGER.error(_ERROR, e);
				}
			}
			if (newResource.getAttach() != null) {
				this.getResourceImageUtil().moveAttachment(newResource, resource);
			}
			this.getResourceService().saveOrUpdate(resource);
			resource.setSkills(getSkills(resource.getTaxonomySet()));
			collectionItem.setResource(resource);
			this.getCollectionRepository().save(collectionItem);
			collectionItem.setStandards(this.getStandards(resource.getTaxonomySet(), false, null));
			getAsyncExecutor().deleteFromCache(V2_ORGANIZE_DATA + collectionItem.getCollection().getUser().getPartyUid() + "*");
		}
		
		this.getCollectionEventLog().getEventLogs(collectionItem, data, user);
		return new ActionResponseDTO<CollectionItem>(collectionItem, errors);
	}

	public ActionResponseDTO<CollectionItem> createCollectionItem(final Resource resource, final Collection collection, final String start, final String stop, final User user) throws Exception {
		final CollectionItem collectionItem = new CollectionItem();
		collectionItem.setCollection(collection);
		collectionItem.setResource(resource);
		collectionItem.setItemType(ShelfType.AddedType.ADDED.getAddedType());
		collectionItem.setAssociatedUser(user);
		collectionItem.setAssociationDate(new Date(System.currentTimeMillis()));
		final int sequence = collectionItem.getCollection().getCollectionItems() != null ? collectionItem.getCollection().getCollectionItems().size() + 1 : 1;
		collectionItem.setItemSequence(sequence);
		collectionItem.getCollection().setItemCount(sequence);
		collectionItem.setStart(start);
		collectionItem.setStop(stop);
		final Errors errors = validateCollectionItem(collection, resource, collectionItem);
		this.getResourceRepository().save(collectionItem);
		getAsyncExecutor().deleteFromCache(V2_ORGANIZE_DATA + collectionItem.getCollection().getUser().getPartyUid() + "*");
		return new ActionResponseDTO<CollectionItem>(collectionItem, errors);
	}

	@Override
	public List<Collection> getMyCollection(final String limit, final String offset, final String orderBy, final String fetchType, final String resourceType, final User user) {
		return getCollectionRepository().getMyCollection(Integer.parseInt(limit), Integer.parseInt(offset), orderBy, fetchType, resourceType, user);
	}

	@Override
	public List<CollectionItem> getMyCollectionItems(final String partyUid, final Map<String, String> filters, final User user) {
		User party = null;
		if (partyUid != null && !partyUid.equalsIgnoreCase(MY)) {
			party = userService.findByGooruId(partyUid);
			if (party != null) {
				filters.put(SHARING, PUBLIC);
			}
		}
		return getCollectionRepository().getMyCollectionItems(filters, party != null ? party : user);
	}

	@Override
	public List<CollectionItem> getCollectionItems(String collectionId, Integer offset, final Integer limit, final String orderBy, String type) {
		return this.getCollectionRepository().getCollectionItems(collectionId, offset, limit, orderBy, "classpage");
	}

	@Override
	public Map<String, Object> getCollection(final String gooruOid, final Map<String, Object> collection, final String rootNodeId) {
		final Collection collectionObj = this.getCollectionRepository().getCollectionByGooruOid(gooruOid, null);
		collection.put(METAINFO, setMetaData(collectionObj, false, rootNodeId));
		final Set<CollectionItem> collectionItems = new TreeSet<CollectionItem>();
		for (CollectionItem collectionItem : collectionObj.getCollectionItems()) {
			collectionItem.getResource().setRatings(this.setRatingsObj(this.getResourceRepository().getResourceSummaryById(collectionItem.getResource().getGooruOid())));
			this.getResourceService().setContentProvider(collectionItem.getResource());
			collectionItems.add(collectionItem);
		}
		collection.put(COLLECTIONITEMS, collectionItems);
		collection.put(GOALS, collectionObj.getGoals());
		collection.put(THUMBNAILS, collectionObj.getThumbnails());
		collection.put(GOORU_OID, collectionObj.getGooruOid());
		collection.put(TITLE, collectionObj.getTitle());
		return collection;
	}

	@Override
	public List<Map<String, String>> getParentCollection(final String collectionGooruOid, final String gooruUid, final boolean reverse) {
		final List<Map<String, String>> parentNode = new ArrayList<Map<String, String>>();
		getCollection(collectionGooruOid, gooruUid, parentNode);
		if (reverse) {
			return parentNode.size() > 0 ? Lists.reverse(parentNode) : parentNode;
		} else {
			return parentNode;
		}
	}

	private List<Map<String, String>> getCollection(final String collectionGooruOid, final String gooruUid, final List<Map<String, String>> parentNode) {
		final Object[] result = this.getCollectionRepository().getParentCollection(collectionGooruOid, gooruUid);
		if (result != null) {
			final Map<String, String>  node = new HashMap<String, String>();
			node.put(GOORU_OID, String.valueOf(result[0]));
			node.put(TITLE, String.valueOf(result[1]));
			parentNode.add(node);
			getCollection(String.valueOf(result[0]), gooruUid, parentNode);
		}
		return parentNode;

	}

	@Override
	public void updateFolderSharing(final String gooruOid) {
		final Resource collection = this.getResourceRepository().findResourceByContent(gooruOid);
		if (collection != null) {
			if (this.getCollectionRepository().getPublicCollectionCount(collection.getGooruOid(), PUBLIC) > 0) {
				collection.setSharing(Sharing.PUBLIC.getSharing());
			} else if (this.getCollectionRepository().getPublicCollectionCount(collection.getGooruOid(), Sharing.ANYONEWITHLINK.getSharing()) > 0) {
				collection.setSharing(Sharing.ANYONEWITHLINK.getSharing());
			} else {
				collection.setSharing(Sharing.PRIVATE.getSharing());
			}
			this.getCollectionRepository().save(collection);
		}

	}

	private ResourceMetaInfo setMetaData(final Collection collection, final boolean ignoreUserTaxonomyPreference, final String rootNodeId) {
		ResourceMetaInfo collectionMetaInfo = null;
		if (collection != null && collection.getTaxonomySet() != null) {
			collectionMetaInfo = new ResourceMetaInfo();
			collectionMetaInfo.setStandards(this.getStandards(collection.getTaxonomySet(), ignoreUserTaxonomyPreference, rootNodeId));
		}
		return collectionMetaInfo;
	}

	private void saveOrUpdateCollectionGrade(final String newResourceGrade, final Resource newResource, final Boolean merge) {
		if (newResource.getGrade() != null && merge) {
			String grade = newResource.getGrade();
			final String resourceGrade = newResourceGrade;
			final List<String> newResourceGrades = Arrays.asList(grade.split(","));
			if (resourceGrade != null) {
				final List<String> resourceGrades = Arrays.asList(resourceGrade.split(","));
				if (resourceGrades != null) {
					for (String newGrade : resourceGrades) {
						if (!newResourceGrades.contains(newGrade) && newGrade.length() > 0) {
							grade += "," + newGrade;
						}
					}
				}
			}
			newResource.setGrade(grade);
		} else {
			newResource.setGrade(newResourceGrade);
		}
		this.getResourceRepository().save(newResource);
	}

	public void deleteBulkCollections(final List<String> gooruOids) {
		final List<Collection> collections = collectionRepository.getCollectionListByIds(gooruOids);
		final StringBuffer removeContentIds = new StringBuffer();
		for (Collection collection : collections) {
			removeContentIds.append(collection.getGooruOid());
		}
		this.collectionRepository.removeAll(collections);
		indexHandler.setReIndexRequest(removeContentIds.toString(), IndexProcessor.DELETE, SCOLLECTION, null, false, false);				
	}

	public void resetFolderVisibility(final String gooruOid, final String gooruUid)  { 
		final List<Map<String, String>> parenFolders = this.getParentCollection(gooruOid, gooruUid, false);
		for (Map<String, String> folder : parenFolders) {
			updateFolderSharing(folder.get(GOORU_OID));
		}
	}
	
	
	public boolean isResourceType(final Resource resource) {
		boolean isResourceType = false;
		if (!resource.getResourceType().equals(ResourceType.Type.SCOLLECTION.getType()) && !resource.getResourceType().equals(ResourceType.Type.CLASSPAGE.getType()) && !resource.getResourceType().equals(ResourceType.Type.FOLDER.getType())) {
			isResourceType = true;
		}
		return isResourceType;
	}

	public ResourceCassandraService getResourceCassandraService() {
		return resourceCassandraService;
	}

	public ResourceService getResourceService() {
		return resourceService;
	}

	public ResourceImageUtil getResourceImageUtil() {
		return resourceImageUtil;
	}

	public CollectionRepository getCollectionRepository() {
		return collectionRepository;
	}

	public CollectionEventLog getCollectionEventLog() {
		return collectionEventLog;
	}

	public ResourceRepository getResourceRepository() {
		return resourceRepository;
	}

	public TaxonomyRespository getTaxonomyRepository() {
		return taxonomyRepository;
	}

	public AssessmentService getAssessmentService() {
		return assessmentService;
	}

	public ContentRepositoryHibernate getContentRepositoryHibernate() {
		return contentRepositoryHibernate;
	}

	public ContentService getContentService() {
		return contentService;
	}

	public FeedbackService getFeedbackService() {
		return feedbackService;
	}

	public OperationAuthorizer getOperationAuthorizer() {
		return operationAuthorizer;
	}

	public AsyncExecutor getAsyncExecutor() {
		return asyncExecutor;
	}

	public CustomTableRepository getCustomTableRepository() {
		return customTableRepository;
	}

	public MailAsyncExecutor getMailAsyncExecutor() {
		return mailAsyncExecutor;
	}

	public CommentRepository getCommentRepository() {
		return commentRepository;
	}

	public CollaboratorRepository getCollaboratorRepository() {
		return collaboratorRepository;
	}

	public CustomFieldsService getCustomFieldsService() {
		return customFieldsService;
	}

	public UserRepository getUserRepository() {
		return userRepository;
	}

	public ResourceManager getResourceManager() {
		return resourceManager;
	}

	public PartyService getPartyService() {
		return partyService;
	}
	
	public ClasspageEventLog getClasspageEventLog() {
		return classpageEventLog;
	}
}
