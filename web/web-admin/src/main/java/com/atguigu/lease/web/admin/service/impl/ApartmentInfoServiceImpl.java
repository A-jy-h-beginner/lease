package com.atguigu.lease.web.admin.service.impl;

import com.atguigu.lease.common.exception.LeaseException;
import com.atguigu.lease.common.result.ResultCodeEnum;
import com.atguigu.lease.model.entity.*;
import com.atguigu.lease.model.enums.ItemType;
import com.atguigu.lease.web.admin.mapper.*;
import com.atguigu.lease.web.admin.service.*;
import com.atguigu.lease.web.admin.vo.apartment.ApartmentDetailVo;
import com.atguigu.lease.web.admin.vo.apartment.ApartmentItemVo;
import com.atguigu.lease.web.admin.vo.apartment.ApartmentQueryVo;
import com.atguigu.lease.web.admin.vo.apartment.ApartmentSubmitVo;
import com.atguigu.lease.web.admin.vo.fee.FeeValueVo;
import com.atguigu.lease.web.admin.vo.graph.GraphVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.minio.messages.Item;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author liubo
 * @description 针对表【apartment_info(公寓信息表)】的数据库操作Service实现
 * @createDate 2023-07-24 15:48:00
 */
@Service
public class ApartmentInfoServiceImpl extends ServiceImpl<ApartmentInfoMapper, ApartmentInfo>
        implements ApartmentInfoService {
    @Autowired
    private GraphInfoService graphInfoService;
    @Autowired
    private ApartmentFacilityService apartmentFacilityService;
    @Autowired
    private ApartmentLabelService apartmentLabelService;
    @Autowired
    private ApartmentFeeValueService apartmentFeeValueService;
    @Autowired
    private LabelInfoService labelInfoService;
    @Autowired
    private FacilityInfoService facilityInfoService;
    @Autowired
    private RoomInfoService roomInfoService;
    // mapper
    @Autowired
    private ApartmentInfoMapper apartmentInfoMapper;
    @Autowired
    private GraphInfoMapper graphInfoMapper;
    @Autowired
    private LabelInfoMapper labelInfoMapper;
    @Autowired
    private FacilityInfoMapper facilityInfoMapper;
    @Autowired
    private FeeValueMapper feeValueMapper;
    @Override
    public void saveOrUpdateApartment(ApartmentSubmitVo apartmentSubmitVo) {
        // 是否新增 -> 看id
        boolean isUpdate = apartmentSubmitVo.getId() != null;
        saveOrUpdate(apartmentSubmitVo); // 是基于ApartmentInfo的IService接口只处理继承的ApartmentInfo部分，扩展的需要处理
        if(isUpdate){
            //1.删除图片，配套，标签，杂费
            graphInfoService.lambdaUpdate()
                    .eq(GraphInfo::getItemType, ItemType.APARTMENT)
                    .eq(GraphInfo::getItemId, apartmentSubmitVo.getId())
                    .remove();
            apartmentFacilityService.lambdaUpdate()
                    .eq(ApartmentFacility::getApartmentId, apartmentSubmitVo.getId())
                    .remove();
            apartmentLabelService.lambdaUpdate()
                    .eq(ApartmentLabel::getApartmentId, apartmentSubmitVo.getId())
                    .remove();
            apartmentFeeValueService.lambdaUpdate()
                    .eq(ApartmentFeeValue::getApartmentId, apartmentSubmitVo.getId())
                    .remove();
        }
        //插入图片
        List<GraphVo> graphVoList = apartmentSubmitVo.getGraphVoList();
        ArrayList<GraphInfo> graphInfos = new ArrayList<>();
//        BeanUtils.copyProperties(graphInfos, graphVoList);
        if(!CollectionUtils.isEmpty(graphVoList)){
            for (GraphVo graphVo : graphVoList) {
//                GraphInfo graphInfo = new GraphInfo();
//                graphInfo.setItemType(ItemType.APARTMENT);
//                graphInfo.setItemId(apartmentSubmitVo.getId());
//                graphInfo.setName(graphVo.getName());
//                graphInfo.setUrl(graphVo.getUrl());
                GraphInfo graphInfo = GraphInfo.builder()
                        .name(graphVo.getName())
                        .url(graphVo.getUrl())
                        .itemId(apartmentSubmitVo.getId()).itemType(ItemType.APARTMENT).build();
                graphInfos.add(graphInfo);
            }
        }
        graphInfoService.saveBatch(graphInfos);
        //插入配套
        List<Long> facilityInfoIds = apartmentSubmitVo.getFacilityInfoIds();
        ArrayList<ApartmentFacility> apartmentFacilities = new ArrayList<>();
        if(!CollectionUtils.isEmpty(facilityInfoIds)){
            for(Long facilityInfoId: facilityInfoIds){
                ApartmentFacility apartmentFacility = ApartmentFacility.builder()
                        .apartmentId(apartmentSubmitVo.getId())
                        .facilityId(facilityInfoId).build();
                apartmentFacilities.add(apartmentFacility);
            }
            apartmentFacilityService.saveBatch(apartmentFacilities);
        }
        //3.插入标签列表
        List<Long> labelIds = apartmentSubmitVo.getLabelIds();
        if (!CollectionUtils.isEmpty(labelIds)) {
            List<ApartmentLabel> apartmentLabelList = new ArrayList<>();
            for (Long labelId : labelIds) {
                ApartmentLabel apartmentLabel = ApartmentLabel.builder()
                        .apartmentId(apartmentSubmitVo.getId())
                        .labelId(labelId).build();
                apartmentLabelList.add(apartmentLabel);
            }
            apartmentLabelService.saveBatch(apartmentLabelList);
        }


        //4.插入杂费列表
        List<Long> feeValueIds = apartmentSubmitVo.getFeeValueIds();
        if (!CollectionUtils.isEmpty(feeValueIds)) {
            ArrayList<ApartmentFeeValue> apartmentFeeValueList = new ArrayList<>();
            for (Long feeValueId : feeValueIds) {
                ApartmentFeeValue apartmentFeeValue = ApartmentFeeValue.builder()
                        .apartmentId(apartmentSubmitVo.getId())
                        .feeValueId(feeValueId).build();
                apartmentFeeValueList.add(apartmentFeeValue);
            }
            apartmentFeeValueService.saveBatch(apartmentFeeValueList);
        }
    }

    @Override
    public IPage<ApartmentItemVo> pageItem(Page<ApartmentItemVo> apartmentItemVoPage, ApartmentQueryVo queryVo) {
        return apartmentInfoMapper.pageItem(apartmentItemVoPage, queryVo);
    }

    @Override
    public ApartmentDetailVo getDetailById(Long id) {
        // 1.查询公寓
        ApartmentInfo apartmentInfo = getById(id);
        // 可以基于自定义mapper查，下面用mp
        // 2.查询图片
        /*List<GraphInfo> graphInfos = graphInfoService.lambdaQuery()
                .eq(GraphInfo::getItemType, ItemType.APARTMENT)
                .eq(GraphInfo::getItemId, id)
                .list();
        // BeanUtils.copyProperties只能copy一个
        List<GraphVo> graphVos = new ArrayList<>();
        for (GraphInfo graphInfo : graphInfos) {
            GraphVo graphVo = new GraphVo();
            BeanUtils.copyProperties(graphVo, graphInfo);
            graphVos.add(graphVo);
        }*/
        // 通用可查公寓房间
        List<GraphVo> graphVoList = graphInfoMapper.selectBYItemTypeAndId(ItemType.APARTMENT, id);
        // 3.查询标签
        /*
        List<ApartmentLabel> apartmentlabelList = apartmentLabelService.lambdaQuery()
                .eq(ApartmentLabel::getApartmentId, id)
                .list();
        ArrayList<String> ids = new ArrayList<>();
        for (ApartmentLabel label : apartmentlabelList) {
            ids.add(String.valueOf(label.getLabelId()));
        }
        List<LabelInfo> labelList = labelInfoService.lambdaQuery()
                .in(LabelInfo::getId, ids)
                .list();*/
        List<LabelInfo> labelInfoList = labelInfoMapper.selectByApartmentId(id);
        // 4.查询配套
        /*
        List<ApartmentFacility> facilityIdList = apartmentFacilityService.lambdaQuery()
                .eq(ApartmentFacility::getApartmentId, id)
                .list();
        List<FacilityInfo> facilityList = facilityInfoService.lambdaQuery()
                .in(FacilityInfo::getId, facilityIdList)
                .list();*/
        List<FacilityInfo> facilityInfoList = facilityInfoMapper.selectByApartmentId(id);
        // 5.查询杂费
        /*
        List<ApartmentFeeValue> feeValueIdList = apartmentFeeValueService.lambdaQuery()
                .eq(ApartmentFeeValue::getApartmentId, id)
                .list();*/
        List<FeeValueVo> feeValueVoList = feeValueMapper.selectByApartId(id);
        // 6.组装结果
        ApartmentDetailVo apartmentDetailVo = new ApartmentDetailVo();
        BeanUtils.copyProperties(apartmentInfo, apartmentDetailVo);
        apartmentDetailVo.setGraphVoList(graphVoList);
        apartmentDetailVo.setLabelInfoList(labelInfoList);
        apartmentDetailVo.setFacilityInfoList(facilityInfoList);
        apartmentDetailVo.setFeeValueVoList(feeValueVoList);
        return apartmentDetailVo;
    }

    @Override
    public void removeApartmentById(Long id) {
        // 判断公寓是否有房间
        Long count = roomInfoService.lambdaQuery()
                .eq(RoomInfo::getApartmentId, id)
                .count();
        if(count > 0){
            throw new LeaseException(ResultCodeEnum.ADMIN_APARTMENT_DELETE_ERROR);
        }
        // 删除公寓信息
        removeById(id);
        //1.删除图片，配套，标签，杂费
        graphInfoService.lambdaUpdate()
                .eq(GraphInfo::getItemType, ItemType.APARTMENT)
                .eq(GraphInfo::getItemId, id)
                .remove();
        apartmentFacilityService.lambdaUpdate()
                .eq(ApartmentFacility::getApartmentId, id)
                .remove();
        apartmentLabelService.lambdaUpdate()
                .eq(ApartmentLabel::getApartmentId, id)
                .remove();
        apartmentFeeValueService.lambdaUpdate()
                .eq(ApartmentFeeValue::getApartmentId, id)
                .remove();
    }

}




