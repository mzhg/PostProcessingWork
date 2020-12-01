package jet.opengl.demos.amdfx.sssr;

import org.lwjgl.util.vector.Matrix4f;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.Numeric;

class ReflectionViewGL {
    /**
     The available timestamp queries.
     */
    enum TimestampQuery
    {
        kTimestampQuery_Init,
        kTimestampQuery_TileClassification,
        kTimestampQuery_Intersection,
        kTimestampQuery_Denoising,

        kTimestampQuery_Count
    };


    void Create(ContextGL context, FfxSssrCreateReflectionViewInfo create_reflection_view_info){
        assert (create_reflection_view_info.pGLCreateReflectionViewInfo != null);
        assert(create_reflection_view_info.pGLCreateReflectionViewInfo.sceneFormat != 0);
        assert(create_reflection_view_info.pGLCreateReflectionViewInfo.depthBufferHierarchySRV != null);
        assert(create_reflection_view_info.pGLCreateReflectionViewInfo.motionBufferSRV != null);
        assert(create_reflection_view_info.pGLCreateReflectionViewInfo.normalBufferSRV != null);
        assert(create_reflection_view_info.pGLCreateReflectionViewInfo.roughnessBufferSRV != null);
        assert(create_reflection_view_info.pGLCreateReflectionViewInfo.normalHistoryBufferSRV != null);
        assert(create_reflection_view_info.pGLCreateReflectionViewInfo.roughnessHistoryBufferSRV != null);
        assert(create_reflection_view_info.pGLCreateReflectionViewInfo.environmentMapSRV != null);
        assert(create_reflection_view_info.pGLCreateReflectionViewInfo.environmentMapSampler != null);
        assert(create_reflection_view_info.pGLCreateReflectionViewInfo.reflectionViewUAV != null);
//        assert(create_reflection_view_info.pGLCreateReflectionViewInfo.uploadCommandBuffer);
        assert(create_reflection_view_info.outputWidth > 0 && create_reflection_view_info.outputHeight > 0);

        // Populate the reflection view properties
        width_ = create_reflection_view_info.outputWidth;
        height_ = create_reflection_view_info.outputHeight;
        flags_ = create_reflection_view_info.flags;
        scene_format_ = create_reflection_view_info.pGLCreateReflectionViewInfo.sceneFormat;

        /*  todo
        // Create pool for timestamp queries
        VkQueryPoolCreateInfo query_pool_create_info = { VK_STRUCTURE_TYPE_QUERY_POOL_CREATE_INFO };
        query_pool_create_info.pNext = nullptr;
        query_pool_create_info.flags = 0;
        query_pool_create_info.queryType = VK_QUERY_TYPE_TIMESTAMP;
        query_pool_create_info.queryCount = kTimestampQuery_Count * context.GetFrameCountBeforeReuse();
        query_pool_create_info.pipelineStatistics = 0;
        if (VK_SUCCESS != vkCreateQueryPool(device_, &query_pool_create_info, NULL, &timestamp_query_pool_))
        {
            throw reflection_error(context, FFX_SSSR_STATUS_INTERNAL_ERROR, "Failed to create timestamp query pool");
        }

        timestamp_queries_.resize(context.GetFrameCountBeforeReuse());
        for (auto& timestamp_queries : timestamp_queries_)
        {
            timestamp_queries.reserve(kTimestampQuery_Count);
        }*/

        // Create reflection view resources
 //       CreateDescriptorPool(context);
        SetupInternalResources(context, create_reflection_view_info);
 //       AllocateDescriptorSets(context);
 //       InitializeResourceDescriptorSets(context, create_reflection_view_info);
    }

    int GetConservativeResourceDescriptorCount(ContextGL context){
        int resource_descriptor_count = context.GetTileClassificationPass().bindings_count_
                + vk_context->GetIndirectArgsPass().bindings_count_
                + vk_context->GetIntersectionPass().bindings_count_
                + vk_context->GetSpatialDenoisingPass().bindings_count_
                + vk_context->GetTemporalDenoisingPass().bindings_count_
                + vk_context->GetEawDenoisingPass().bindings_count_;
        resource_descriptor_count *= 2; // double buffering descriptors
        return resource_descriptor_count;
    }

    /**
     Creates all internal resources and handles initial resource transitions.

     @param context The context to be used.
     @param create_reflection_view_info The reflection view to be resolved.

     */
    void SetupInternalResources(ContextGL context, FfxSssrCreateReflectionViewInfo create_reflection_view_info){
        /*VkSamplerCreateInfo sampler_info = { VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO };
        sampler_info.pNext = nullptr;
        sampler_info.flags = 0;
        sampler_info.magFilter = VK_FILTER_LINEAR;
        sampler_info.minFilter = VK_FILTER_LINEAR;
        sampler_info.mipmapMode = VK_SAMPLER_MIPMAP_MODE_LINEAR;
        sampler_info.addressModeU = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_BORDER;
        sampler_info.addressModeV = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_BORDER;
        sampler_info.addressModeW = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_BORDER;
        sampler_info.mipLodBias = 0;
        sampler_info.anisotropyEnable = false;
        sampler_info.maxAnisotropy = 0;
        sampler_info.compareEnable = false;
        sampler_info.compareOp = VK_COMPARE_OP_NEVER;
        sampler_info.minLod = 0;
        sampler_info.maxLod = 16;
        sampler_info.borderColor = VK_BORDER_COLOR_FLOAT_TRANSPARENT_BLACK;
        sampler_info.unnormalizedCoordinates = false;
        if (VK_SUCCESS != vkCreateSampler(device_, &sampler_info, nullptr, &linear_sampler_))
        {
            throw reflection_error(context, FFX_SSSR_STATUS_INTERNAL_ERROR, "Failed to create linear sampler");
        }*/

        SamplerDesc sampler_info = new SamplerDesc();
        sampler_info.minFilter = GLenum.GL_LINEAR;
        sampler_info.magFilter = GLenum.GL_LINEAR;
        sampler_info.wrapS =sampler_info.wrapT = sampler_info.wrapR = GLenum.GL_CLAMP_TO_BORDER;
        sampler_info.borderColor = 0;
        linear_sampler_ = SamplerUtils.createSampler(sampler_info);

        // Create tile classification-related buffers
        {
            int num_tiles = Numeric.divideAndRoundUp(width_, 8) * Numeric.divideAndRoundUp(height_, 8);
            int num_pixels = width_ * height_;

            int tile_list_element_count = num_tiles;
            int tile_counter_element_count = 1;
            int ray_list_element_count = num_pixels;
            int ray_counter_element_count = 1;
            int intersection_pass_indirect_args_element_count = 3;
            int denoiser_pass_indirect_args_element_count = 3;

            /*BufferVK::CreateInfo create_info = {};
            create_info.memory_property_flags = VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT;
            create_info.format_ = VK_FORMAT_R32_UINT;
            create_info.buffer_usage_ = VK_BUFFER_USAGE_STORAGE_TEXEL_BUFFER_BIT | VK_BUFFER_USAGE_UNIFORM_TEXEL_BUFFER_BIT;

            create_info.size_in_bytes_ = tile_list_element_count * sizeof(uint32_t);
            create_info.name_ = "SSSR Tile List";
            tile_list_ = BufferVK(device_, physical_device_, create_info);*/

            tile_list_ = new BufferGL();
            tile_list_.initlize(GLenum.GL_UNIFORM_BUFFER, tile_list_element_count * 4, null, GLenum.GL_STREAM_COPY);

            /*create_info.size_in_bytes_ = ray_list_element_count * sizeof(uint32_t);
            create_info.name_ = "SSSR Ray List";
            ray_list_ = BufferVK(device_, physical_device_, create_info);*/

            ray_list_ = new BufferGL();
            ray_list_.initlize(GLenum.GL_UNIFORM_BUFFER, ray_list_element_count * 4, null, GLenum.GL_STREAM_COPY);

            /*create_info.buffer_usage_ = VK_BUFFER_USAGE_STORAGE_TEXEL_BUFFER_BIT | VK_BUFFER_USAGE_UNIFORM_TEXEL_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT;

            create_info.size_in_bytes_ = tile_counter_element_count * sizeof(uint32_t);
            create_info.name_ = "SSSR Tile Counter";
            tile_counter_ = BufferVK(device_, physical_device_, create_info);*/
            tile_counter_ = new BufferGL();
            tile_counter_.initlize(GLenum.GL_UNIFORM_BUFFER, tile_counter_element_count * 4, null, GLenum.GL_STREAM_COPY);

            /*create_info.size_in_bytes_ = ray_counter_element_count * sizeof(uint32_t);
            create_info.name_ = "SSSR Ray Counter";
            ray_counter_ = BufferVK(device_, physical_device_, create_info);*/
            ray_counter_ = new BufferGL();
            ray_counter_.initlize(GLenum.GL_UNIFORM_BUFFER, ray_counter_element_count * 4, null, GLenum.GL_STREAM_COPY);

            /*create_info.buffer_usage_ = VK_BUFFER_USAGE_STORAGE_TEXEL_BUFFER_BIT | VK_BUFFER_USAGE_INDIRECT_BUFFER_BIT;

            create_info.size_in_bytes_ = intersection_pass_indirect_args_element_count * sizeof(uint32_t);
            create_info.name_ = "SSSR Intersect Indirect Args";
            intersection_pass_indirect_args_ = BufferVK(device_, physical_device_, create_info);*/
            intersection_pass_indirect_args_ = new BufferGL();
            intersection_pass_indirect_args_.initlize(GLenum.GL_UNIFORM_BUFFER, intersection_pass_indirect_args_element_count * 4, null, GLenum.GL_STREAM_COPY);

            /*create_info.size_in_bytes_ = denoiser_pass_indirect_args_element_count * sizeof(uint32_t);
            create_info.name_ = "SSSR Denoiser Indirect Args";
            denoiser_pass_indirect_args_ = BufferVK(device_, physical_device_, create_info);*/
            denoiser_pass_indirect_args_ = new BufferGL();
            denoiser_pass_indirect_args_.initlize(GLenum.GL_UNIFORM_BUFFER, denoiser_pass_indirect_args_element_count * 4, null, GLenum.GL_STREAM_COPY);
        }

        // Create denoising-related resources
        {
            Texture2DDesc create_info = new Texture2DDesc();
            create_info.width = width_;
            create_info.height = height_;
            create_info.mipLevels = 1;
//            create_info.initial_layout_ = VK_IMAGE_LAYOUT_UNDEFINED;
//            create_info.memory_property_flags = VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT;
//            create_info.image_usage_ = VK_IMAGE_USAGE_STORAGE_BIT | VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT;

            create_info.format = scene_format_;
//            create_info.name_ = "SSSR Temporal Denoised Result 0";
            temporal_denoiser_result_[0] = TextureUtils.createTexture2D(create_info, null);

            create_info.format = scene_format_;
//            create_info.name_ = "SSSR Temporal Denoised Result 1";
            temporal_denoiser_result_[1] = TextureUtils.createTexture2D(create_info, null);

            create_info.format = GLenum.GL_R16F;
//            create_info.name_ = "SSSR Ray Lengths";
            ray_lengths_ = TextureUtils.createTexture2D(create_info, null);

            create_info.format = GLenum.GL_R8;
//            create_info.name_ = "SSSR Temporal Variance";
            temporal_variance_ = TextureUtils.createTexture2D(create_info, null);
        }

        /*VkCommandBuffer command_buffer = create_reflection_view_info.pVkCreateReflectionViewInfo->uploadCommandBuffer;

        VkImageMemoryBarrier image_barriers[] = {
                Transition(temporal_denoiser_result_[0].image_, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_GENERAL),
                Transition(temporal_denoiser_result_[1].image_, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_GENERAL),
                Transition(ray_lengths_.image_, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_GENERAL),
                Transition(temporal_variance_.image_, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_GENERAL)
        };
        TransitionBarriers(command_buffer, image_barriers, FFX_SSSR_ARRAY_SIZE(image_barriers));

        // Initial clear of counters. Successive clears are handled by the indirect arguments pass.
        vkCmdFillBuffer(command_buffer, ray_counter_.buffer_, 0, VK_WHOLE_SIZE, 0);
        vkCmdFillBuffer(command_buffer, tile_counter_.buffer_, 0, VK_WHOLE_SIZE, 0);

        VkClearColorValue clear_calue = {};
        clear_calue.float32[0] = 0;
        clear_calue.float32[1] = 0;
        clear_calue.float32[2] = 0;
        clear_calue.float32[3] = 0;

        VkImageSubresourceRange subresource_range = {};
        subresource_range.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
        subresource_range.baseArrayLayer = 0;
        subresource_range.baseMipLevel = 0;
        subresource_range.layerCount = 1;
        subresource_range.levelCount = 1;

        // Initial resource clears
        vkCmdClearColorImage(command_buffer, temporal_denoiser_result_[0].image_, VK_IMAGE_LAYOUT_GENERAL, &clear_calue, 1, &subresource_range);
        vkCmdClearColorImage(command_buffer, temporal_denoiser_result_[1].image_, VK_IMAGE_LAYOUT_GENERAL, &clear_calue, 1, &subresource_range);
        vkCmdClearColorImage(command_buffer, ray_lengths_.image_, VK_IMAGE_LAYOUT_GENERAL, &clear_calue, 1, &subresource_range);
        vkCmdClearColorImage(command_buffer, temporal_variance_.image_, VK_IMAGE_LAYOUT_GENERAL, &clear_calue, 1, &subresource_range);
        */
    }

    /*void AllocateDescriptorSets(Context& context);
    VkDescriptorSet AllocateDescriptorSet(Context& context, VkDescriptorSetLayout layout);
    void InitializeResourceDescriptorSets(Context& context, FfxSssrCreateReflectionViewInfo const& create_reflection_view_info);*/

    long GetTimestampQueryIndex(){
        return timestamp_queries_index_ * kTimestampQuery_Count + static_cast<std::uint32_t>(timestamp_queries_[timestamp_queries_index_].size());
    }

    private final PassData pass_data = new PassData();
    void Resolve(ContextGL context, Matrix4f view, Matrix4f proj, FfxSssrResolveReflectionViewInfo resolve_reflection_view_info){
        // todo time query

        BufferGL upload_buffer = context.GetUploadBuffer();
        /*if (!upload_buffer.AllocateBuffer(sizeof(PassData), pass_data))
        {
            throw reflection_error(context, FFX_SSSR_STATUS_OUT_OF_MEMORY, "Failed to allocate %u bytes of upload memory, consider increasing uploadBufferSize", sizeof(PassData));
        }*/

        // Fill constant buffer
        /*matrix4 view_projection = reflection_view.projection_matrix_ * reflection_view.view_matrix_;
        pass_data->inv_view_projection_ = matrix4::inverse(view_projection);
        pass_data->projection_ = reflection_view.projection_matrix_;
        pass_data->inv_projection_ = matrix4::inverse(reflection_view.projection_matrix_);
        pass_data->view_ = reflection_view.view_matrix_;
        pass_data->inv_view_ = matrix4::inverse(reflection_view.view_matrix_);*/
        pass_data.projection_.load(proj);
        pass_data.view_.load(view);
        Matrix4f.invert(proj, pass_data.inv_projection_);

        pass_data->prev_view_projection_ = prev_view_projection_;

        pass_data->frame_index_ = context.GetFrameIndex();

        float temporal_stability_scale = Clamp(resolve_reflection_view_info.temporalStabilityScale, 0, 1);
        pass_data->max_traversal_intersections_ = resolve_reflection_view_info.maxTraversalIterations;
        pass_data->min_traversal_occupancy_ = resolve_reflection_view_info.minTraversalOccupancy;
        pass_data->most_detailed_mip_ = resolve_reflection_view_info.mostDetailedDepthHierarchyMipLevel;
        pass_data->temporal_stability_factor_ = temporal_stability_scale * temporal_stability_scale;
        pass_data->depth_buffer_thickness_ = resolve_reflection_view_info.depthBufferThickness;
        pass_data->samples_per_quad_ = resolve_reflection_view_info.samplesPerQuad == FFX_SSSR_RAY_SAMPLES_PER_QUAD_4 ? 4 : (resolve_reflection_view_info.samplesPerQuad == FFX_SSSR_RAY_SAMPLES_PER_QUAD_2 ? 2 : 1);
        pass_data->temporal_variance_guided_tracing_enabled_ = resolve_reflection_view_info.flags & FFX_SSSR_RESOLVE_REFLECTION_VIEW_FLAG_ENABLE_VARIANCE_GUIDED_TRACING ? 1 : 0;
        pass_data->roughness_threshold_ = resolve_reflection_view_info.roughnessThreshold;
        pass_data->skip_denoiser_ = resolve_reflection_view_info.flags & FFX_SSSR_RESOLVE_REFLECTION_VIEW_FLAG_DENOISE ? 0 : 1;
        prev_view_projection_ = view_projection;

        uint32_t uniform_buffer_index = context.GetFrameIndex() % context.GetFrameCountBeforeReuse();
        VkDescriptorSet uniform_buffer_descriptor_set = uniform_buffer_descriptor_set_[uniform_buffer_index];

        // Update descriptor to sliding window in upload buffer that contains the updated pass data
        {
            VkDescriptorBufferInfo buffer_info = {};
            buffer_info.buffer = upload_buffer.GetResource();
            buffer_info.offset = upload_buffer.GetOffset(pass_data);
            buffer_info.range = sizeof(PassData);

            VkWriteDescriptorSet write_set = { VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET };
            write_set.pNext = nullptr;
            write_set.dstSet = uniform_buffer_descriptor_set;
            write_set.dstBinding = 0;
            write_set.dstArrayElement = 0;
            write_set.descriptorCount = 1;
            write_set.descriptorType = VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER;
            write_set.pImageInfo = nullptr;
            write_set.pBufferInfo = &buffer_info;
            write_set.pTexelBufferView = nullptr;
            vkUpdateDescriptorSets(device_, 1, &write_set, 0, nullptr);
        }

        std::uint32_t resource_descriptor_set_index = context.GetFrameIndex() & 1u;

        ContextVK* vk_context = context.GetContextVK();

        // Tile Classification pass
        {
            VkDescriptorSet sets[] = { uniform_buffer_descriptor_set,  tile_classification_descriptor_set_[resource_descriptor_set_index] };
            vkCmdBindPipeline(command_buffer, VK_PIPELINE_BIND_POINT_COMPUTE, vk_context->GetTileClassificationPass().pipeline_);
            vkCmdBindDescriptorSets(command_buffer, VK_PIPELINE_BIND_POINT_COMPUTE, vk_context->GetTileClassificationPass().pipeline_layout_, 0, FFX_SSSR_ARRAY_SIZE(sets), sets, 0, nullptr);
            uint32_t dim_x = RoundedDivide(width_, 8u);
            uint32_t dim_y = RoundedDivide(height_, 8u);
            vkCmdDispatch(command_buffer, dim_x, dim_y, 1);
        }

        // Ensure that the tile classification pass finished
        ComputeBarrier(command_buffer);

        // Indirect Arguments pass
        {
            VkDescriptorSet sets[] = { uniform_buffer_descriptor_set,  indirect_args_descriptor_set_[resource_descriptor_set_index] };
            vkCmdBindPipeline(command_buffer, VK_PIPELINE_BIND_POINT_COMPUTE, vk_context->GetIndirectArgsPass().pipeline_);
            vkCmdBindDescriptorSets(command_buffer, VK_PIPELINE_BIND_POINT_COMPUTE, vk_context->GetIndirectArgsPass().pipeline_layout_, 0, FFX_SSSR_ARRAY_SIZE(sets), sets, 0, nullptr);
            vkCmdDispatch(command_buffer, 1, 1, 1);
        }

        // Query the amount of time spent in the intersection pass
        if ((flags_ & FFX_SSSR_CREATE_REFLECTION_VIEW_FLAG_ENABLE_PERFORMANCE_COUNTERS) != 0)
        {
            auto& timestamp_queries = timestamp_queries_[timestamp_queries_index_];

            FFX_SSSR_ASSERT(timestamp_queries.size() == 1ull && timestamp_queries[0] == kTimestampQuery_Init);

            vkCmdWriteTimestamp(command_buffer, VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT, timestamp_query_pool_, GetTimestampQueryIndex());
            timestamp_queries.push_back(kTimestampQuery_TileClassification);
        }

        // Ensure that the arguments are written
        IndirectArgumentsBarrier(command_buffer);

        // Intersection pass
        {
            VkDescriptorSet sets[] = { uniform_buffer_descriptor_set,  intersection_descriptor_set_[resource_descriptor_set_index] };
            vkCmdBindPipeline(command_buffer, VK_PIPELINE_BIND_POINT_COMPUTE, vk_context->GetIntersectionPass().pipeline_);
            vkCmdBindDescriptorSets(command_buffer, VK_PIPELINE_BIND_POINT_COMPUTE, vk_context->GetIntersectionPass().pipeline_layout_, 0, FFX_SSSR_ARRAY_SIZE(sets), sets, 0, nullptr);
            vkCmdDispatchIndirect(command_buffer, intersection_pass_indirect_args_.buffer_, 0);
        }

        // Query the amount of time spent in the intersection pass
        if ((flags_ & FFX_SSSR_CREATE_REFLECTION_VIEW_FLAG_ENABLE_PERFORMANCE_COUNTERS) != 0)
        {
            auto& timestamp_queries = timestamp_queries_[timestamp_queries_index_];

            FFX_SSSR_ASSERT(timestamp_queries.size() == 2ull && timestamp_queries[1] == kTimestampQuery_TileClassification);

            vkCmdWriteTimestamp(command_buffer, VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT, timestamp_query_pool_, GetTimestampQueryIndex());
            timestamp_queries.push_back(kTimestampQuery_Intersection);
        }

        if (resolve_reflection_view_info.flags & FFX_SSSR_RESOLVE_REFLECTION_VIEW_FLAG_DENOISE)
        {
            // Ensure that the intersection pass finished
            VkImageMemoryBarrier intersection_finished_barriers[] = {
                    Transition(temporal_denoiser_result_[resource_descriptor_set_index].image_, VK_IMAGE_LAYOUT_GENERAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL),
                    Transition(temporal_variance_.image_, VK_IMAGE_LAYOUT_GENERAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
            };
            TransitionBarriers(command_buffer, intersection_finished_barriers, FFX_SSSR_ARRAY_SIZE(intersection_finished_barriers));

            // Spatial denoiser passes
            {
                VkDescriptorSet sets[] = { uniform_buffer_descriptor_set,  spatial_denoising_descriptor_set_[resource_descriptor_set_index] };
                vkCmdBindPipeline(command_buffer, VK_PIPELINE_BIND_POINT_COMPUTE, vk_context->GetSpatialDenoisingPass().pipeline_);
                vkCmdBindDescriptorSets(command_buffer, VK_PIPELINE_BIND_POINT_COMPUTE, vk_context->GetSpatialDenoisingPass().pipeline_layout_, 0, FFX_SSSR_ARRAY_SIZE(sets), sets, 0, nullptr);
                vkCmdDispatchIndirect(command_buffer, denoiser_pass_indirect_args_.buffer_, 0);
            }

            // Ensure that the spatial denoising pass finished. We don't have the resource for the final result available, thus we have to wait for any UAV access to finish.
            VkImageMemoryBarrier spatial_denoiser_finished_barriers[] = {
                    Transition(temporal_denoiser_result_[resource_descriptor_set_index].image_, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, VK_IMAGE_LAYOUT_GENERAL),
                    Transition(temporal_denoiser_result_[1 - resource_descriptor_set_index].image_, VK_IMAGE_LAYOUT_GENERAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL),
                    Transition(temporal_variance_.image_, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, VK_IMAGE_LAYOUT_GENERAL),
                    Transition(ray_lengths_.image_, VK_IMAGE_LAYOUT_GENERAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
            };
            TransitionBarriers(command_buffer, spatial_denoiser_finished_barriers, FFX_SSSR_ARRAY_SIZE(spatial_denoiser_finished_barriers));

            // Temporal denoiser passes
            {
                VkDescriptorSet sets[] = { uniform_buffer_descriptor_set,  temporal_denoising_descriptor_set_[resource_descriptor_set_index] };
                vkCmdBindPipeline(command_buffer, VK_PIPELINE_BIND_POINT_COMPUTE, vk_context->GetTemporalDenoisingPass().pipeline_);
                vkCmdBindDescriptorSets(command_buffer, VK_PIPELINE_BIND_POINT_COMPUTE, vk_context->GetTemporalDenoisingPass().pipeline_layout_, 0, FFX_SSSR_ARRAY_SIZE(sets), sets, 0, nullptr);
                vkCmdDispatchIndirect(command_buffer, denoiser_pass_indirect_args_.buffer_, 0);
            }

            // Ensure that the temporal denoising pass finished
            VkImageMemoryBarrier temporal_denoiser_finished_barriers[] = {
                    Transition(ray_lengths_.image_, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, VK_IMAGE_LAYOUT_GENERAL),
                    Transition(temporal_denoiser_result_[1 - resource_descriptor_set_index].image_, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, VK_IMAGE_LAYOUT_GENERAL),
            };
            TransitionBarriers(command_buffer, temporal_denoiser_finished_barriers, FFX_SSSR_ARRAY_SIZE(temporal_denoiser_finished_barriers));

            // EAW denoiser passes
            {
                VkDescriptorSet sets[] = { uniform_buffer_descriptor_set,  eaw_denoising_descriptor_set_[resource_descriptor_set_index] };
                vkCmdBindPipeline(command_buffer, VK_PIPELINE_BIND_POINT_COMPUTE, vk_context->GetEawDenoisingPass().pipeline_);
                vkCmdBindDescriptorSets(command_buffer, VK_PIPELINE_BIND_POINT_COMPUTE, vk_context->GetEawDenoisingPass().pipeline_layout_, 0, FFX_SSSR_ARRAY_SIZE(sets), sets, 0, nullptr);
                vkCmdDispatchIndirect(command_buffer, denoiser_pass_indirect_args_.buffer_, 0);
            }

            // Query the amount of time spent in the denoiser passes
            if ((flags_ & FFX_SSSR_CREATE_REFLECTION_VIEW_FLAG_ENABLE_PERFORMANCE_COUNTERS) != 0)
            {
                auto& timestamp_queries = timestamp_queries_[timestamp_queries_index_];

                FFX_SSSR_ASSERT(timestamp_queries.size() == 3ull && timestamp_queries[2] == kTimestampQuery_Intersection);

                vkCmdWriteTimestamp(command_buffer, VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT, timestamp_query_pool_, GetTimestampQueryIndex());
                timestamp_queries.push_back(kTimestampQuery_Denoising);
            }
        }

        // Move timestamp queries to next frame
        if ((flags_ & FFX_SSSR_CREATE_REFLECTION_VIEW_FLAG_ENABLE_PERFORMANCE_COUNTERS) != 0)
        {
            timestamp_queries_index_ = (timestamp_queries_index_ + 1u) % context.GetFrameCountBeforeReuse();
        }
    }

    // The width of the reflection view (in texels).
    int width_;
    // The height of the reflection view (in texels).
    int height_;
    // The reflection view creation flags.
    int flags_;

    // Linear sampler.
    int linear_sampler_;
    // Containing all tiles that need at least one ray.
    BufferGL tile_list_;
    BufferGL tile_counter_;
    // Containing all rays that need to be traced.
    BufferGL ray_list_;
    BufferGL ray_counter_;
    // Indirect arguments for intersection pass.
    BufferGL intersection_pass_indirect_args_;
    // Indirect arguments for denoiser pass.
    BufferGL denoiser_pass_indirect_args_;
    // Intermediate result of the temporal denoising pass - double buffered to keep history and aliases the intersection result.
    final TextureGL[] temporal_denoiser_result_ = new TextureGL[2];
    // Holds the length of each reflection ray - used for temporal reprojection.
    TextureGL ray_lengths_;
    // Holds the temporal variance of the last two frames.
    TextureGL temporal_variance_;

    // The query pool containing the recorded timestamps.
//    VkQueryPool timestamp_query_pool_;
    // The number of GPU ticks spent in the tile classification pass.
    long tile_classification_elapsed_time_;
    // The number of GPU ticks spent in depth buffer intersection.
    long intersection_elapsed_time_;
    // The number of GPU ticks spent denoising.
    long denoising_elapsed_time_;
    // The array of timestamp that were queried.
    std::vector<TimestampQueries> timestamp_queries_;
    // The index of the active set of timestamp queries.
    long timestamp_queries_index_;

    // Format of the resolved scene.
    int scene_format_;


    // The view projection matrix of the last frame.
    final Matrix4f prev_view_projection_ = new Matrix4f();
}
