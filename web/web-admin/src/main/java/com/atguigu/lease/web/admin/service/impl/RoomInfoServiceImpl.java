package com.atguigu.lease.web.admin.service.impl;

import com.atguigu.lease.common.constant.RedisConstant;
import com.atguigu.lease.model.entity.*;
import com.atguigu.lease.model.enums.ItemType;
import com.atguigu.lease.web.admin.mapper.*;
import com.atguigu.lease.web.admin.service.*;
import com.atguigu.lease.web.admin.vo.attr.AttrValueVo;
import com.atguigu.lease.web.admin.vo.graph.GraphVo;
import com.atguigu.lease.web.admin.vo.room.RoomDetailVo;
import com.atguigu.lease.web.admin.vo.room.RoomItemVo;
import com.atguigu.lease.web.admin.vo.room.RoomQueryVo;
import com.atguigu.lease.web.admin.vo.room.RoomSubmitVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author liubo
 * @description 针对表【room_info(房间信息表)】的数据库操作Service实现
 * @createDate 2023-07-24 15:48:00
 */
@Service
public class RoomInfoServiceImpl extends ServiceImpl<RoomInfoMapper, RoomInfo>
        implements RoomInfoService {
    @Autowired
    private GraphInfoService graphInfoService;
    @Autowired
    private RoomFacilityService roomFacilityService;
    @Autowired
    private RoomLabelService roomLabelService;
    @Autowired
    private RoomAttrValueService roomAttrValueService;
    @Autowired
    private RoomPaymentTypeService roomPaymentTypeService;
    @Autowired
    private RoomLeaseTermService roomLeaseTermService;
//    @Autowired  循环依赖
//    private ApartmentInfoService apartmentInfoService;
    // mapper
    @Autowired
    private RoomInfoMapper roomInfoMapper;
    @Autowired
    private GraphInfoMapper graphInfoMapper;
    @Autowired
    private AttrValueMapper attrValueMapper;
    @Autowired
    private FacilityInfoMapper facilityInfoMapper;
    @Autowired
    private LabelInfoMapper labelInfoMapper;
    @Autowired
    private PaymentTypeMapper paymentTypeMapper;
    @Autowired
    private LeaseTermMapper leaseTermMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Override
    public void saveOrUpdateRoom(RoomSubmitVo roomSubmitVo) {
        boolean isUpdate = roomSubmitVo.getId() != null;
        saveOrUpdate(roomSubmitVo);
        if(isUpdate){
            //1.删除图片，配套，标签，杂费,租约类型，支付方式
            graphInfoService.lambdaUpdate()
                    .eq(GraphInfo::getItemType, ItemType.ROOM)
                    .eq(GraphInfo::getItemId, roomSubmitVo.getId())
                    .remove();
            roomFacilityService.lambdaUpdate()
                    .eq(RoomFacility::getRoomId, roomSubmitVo.getId())
                    .remove();
            roomLabelService.lambdaUpdate()
                    .eq(RoomLabel::getRoomId, roomSubmitVo.getId())
                    .remove();
            roomAttrValueService.lambdaUpdate()
                    .eq(RoomAttrValue::getRoomId, roomSubmitVo.getId())
                    .remove();
            roomPaymentTypeService.lambdaUpdate()
                    .eq(RoomPaymentType::getRoomId, roomSubmitVo.getId())
                    .remove();
            roomLeaseTermService.lambdaUpdate()
                    .eq(RoomLeaseTerm::getRoomId, roomSubmitVo.getId())
                    .remove();
            // 删除缓存
            String key = RedisConstant.APP_ROOM_PREFIX + roomSubmitVo.getId();
            redisTemplate.delete(key);
        }
        //1.保存新的graphInfoList
        List<GraphVo> graphVoList = roomSubmitVo.getGraphVoList();
        if (!CollectionUtils.isEmpty(graphVoList)) {
            ArrayList<GraphInfo> graphInfoList = new ArrayList<>();
            for (GraphVo graphVo : graphVoList) {
                GraphInfo graphInfo = GraphInfo.builder()
                        .itemType(ItemType.ROOM)
                        .itemId(roomSubmitVo.getId())
                        .name(graphVo.getName())
                        .url(graphVo.getUrl())
                        .build();
                graphInfoList.add(graphInfo);
            }
            graphInfoService.saveBatch(graphInfoList);
        }

        //2.保存新的roomAttrValueList
        List<Long> attrValueIds = roomSubmitVo.getAttrValueIds();
        if (!CollectionUtils.isEmpty(attrValueIds)) {
            List<RoomAttrValue> roomAttrValueList = new ArrayList<>();
            for (Long attrValueId : attrValueIds) {
                RoomAttrValue roomAttrValue = RoomAttrValue.builder().roomId(roomSubmitVo.getId()).attrValueId(attrValueId).build();
                roomAttrValueList.add(roomAttrValue);
            }
            roomAttrValueService.saveBatch(roomAttrValueList);
        }

        //3.保存新的facilityInfoList
        List<Long> facilityInfoIds = roomSubmitVo.getFacilityInfoIds();
        if (!CollectionUtils.isEmpty(facilityInfoIds)) {
            List<RoomFacility> roomFacilityList = new ArrayList<>();
            for (Long facilityInfoId : facilityInfoIds) {
                RoomFacility roomFacility = RoomFacility.builder().roomId(roomSubmitVo.getId()).facilityId(facilityInfoId).build();
                roomFacilityList.add(roomFacility);
            }
            roomFacilityService.saveBatch(roomFacilityList);
        }

        //4.保存新的labelInfoList
        List<Long> labelInfoIds = roomSubmitVo.getLabelInfoIds();
        if (!CollectionUtils.isEmpty(labelInfoIds)) {
            ArrayList<RoomLabel> roomLabelList = new ArrayList<>();
            for (Long labelInfoId : labelInfoIds) {
                RoomLabel roomLabel = RoomLabel.builder().roomId(roomSubmitVo.getId()).labelId(labelInfoId).build();
                roomLabelList.add(roomLabel);
            }
            roomLabelService.saveBatch(roomLabelList);
        }

        //5.保存新的paymentTypeList
        List<Long> paymentTypeIds = roomSubmitVo.getPaymentTypeIds();
        if (!CollectionUtils.isEmpty(paymentTypeIds)) {
            ArrayList<RoomPaymentType> roomPaymentTypeList = new ArrayList<>();
            for (Long paymentTypeId : paymentTypeIds) {
                RoomPaymentType roomPaymentType = RoomPaymentType.builder().roomId(roomSubmitVo.getId()).paymentTypeId(paymentTypeId).build();
                roomPaymentTypeList.add(roomPaymentType);
            }
            roomPaymentTypeService.saveBatch(roomPaymentTypeList);
        }

        //6.保存新的leaseTermList
        List<Long> leaseTermIds = roomSubmitVo.getLeaseTermIds();
        if (!CollectionUtils.isEmpty(leaseTermIds)) {
            ArrayList<RoomLeaseTerm> roomLeaseTerms = new ArrayList<>();
            for (Long leaseTermId : leaseTermIds) {
                RoomLeaseTerm roomLeaseTerm = RoomLeaseTerm.builder().roomId(roomSubmitVo.getId()).leaseTermId(leaseTermId).build();
                roomLeaseTerms.add(roomLeaseTerm);
            }
            roomLeaseTermService.saveBatch(roomLeaseTerms);
        }
    }

    @Override
    public IPage<RoomItemVo> pageItem(Page<RoomItemVo> apartmentItemVoPage, RoomQueryVo queryVo) {
        return roomInfoMapper.pageItem(apartmentItemVoPage, queryVo);
    }

    @Override
    public RoomDetailVo getRoomDetailById(Long id) {
        // 查询房间信息
        RoomInfo roomInfo = getById(id);
        // 查询公寓信息
        ApartmentInfo apartmentInfo = Db.lambdaQuery(ApartmentInfo.class)
                .eq(ApartmentInfo::getId, roomInfo.getApartmentId())
                .one();
        // 查询房间图片
        List<GraphVo> graphVoList = graphInfoMapper.selectBYItemTypeAndId(ItemType.ROOM, id);
        // 查询房间属性attrValueList
        List<AttrValueVo> attrValueVoList = attrValueMapper.selectListByRoomId(id);
        // 查询房间配套
        List<FacilityInfo> facilityInfoList = facilityInfoMapper.selectListByRoomId(id);
        //6.查询labelInfoList
        List<LabelInfo> labelInfoList = labelInfoMapper.selectListByRoomId(id);
        //7.查询paymentTypeList
        List<PaymentType> paymentTypeList = paymentTypeMapper.selectListByRoomId(id);
        //8.查询leaseTermList
        List<LeaseTerm> leaseTermList = leaseTermMapper.selectListByRoomId(id);
        // 组装结果
        RoomDetailVo roomDetailVo = new RoomDetailVo();
        BeanUtils.copyProperties(roomInfo, roomDetailVo);
        roomDetailVo.setApartmentInfo(apartmentInfo);
        roomDetailVo.setGraphVoList(graphVoList);
        roomDetailVo.setAttrValueVoList(attrValueVoList);
        roomDetailVo.setFacilityInfoList(facilityInfoList);
        roomDetailVo.setLabelInfoList(labelInfoList);
        roomDetailVo.setPaymentTypeList(paymentTypeList);
        roomDetailVo.setLeaseTermList(leaseTermList);
        return roomDetailVo;
    }

    @Override
    public void removeRoomById(Long id) {
        // 删除房间
        removeById(id);
        // 删除房间图片
        graphInfoService.lambdaUpdate()
                .eq(GraphInfo::getItemId, id)
                .eq(GraphInfo::getItemType, ItemType.ROOM)
                .remove();
        // 删除房间属性attrValueList
        roomAttrValueService.lambdaUpdate()
                .eq(RoomAttrValue::getRoomId, id)
                .remove();
        //4.删除facilityInfoList
        roomFacilityService.lambdaUpdate()
                .eq(RoomFacility::getRoomId, id)
                .remove();

        //5.删除labelInfoList
        roomLabelService.lambdaUpdate()
                .eq(RoomLabel::getRoomId, id)
                .remove();

        //6.删除paymentTypeList
        roomPaymentTypeService.lambdaUpdate()
                .eq(RoomPaymentType::getRoomId, id)
                .remove();

        //7.删除leaseTermList
        roomLeaseTermService.lambdaUpdate()
                .eq(RoomLeaseTerm::getRoomId, id)
                .remove();

        // 删除缓存
        String key = RedisConstant.APP_ROOM_PREFIX + id;
        redisTemplate.delete(key);
    }

}




