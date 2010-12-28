/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.metadata;


/**
 * Marker interface for source-agnostic representations of configuration metadata.
 *
 * <p>For example, component-scanning may be configured via XML or the
 * {@link org.springframework.context.annotation.ComponentScan} annotation. These
 * specific source forms are parsed by
 * {@link org.springframework.context.annotation.ComponentScanBeanDefinitionParser} and
 * {@link org.springframework.context.annotation.ComponentScanAnnotationMetadataParser}
 * respectively, but both parse into a
 * {@link org.springframework.context.annotation.ComponentScanMetadata} object (a
 * {@link MetadataDefinition} implementation) which is then read by a
 * {@link org.springframework.context.annotation.ComponentScanMetadataReader} in order
 * to configure a {@link org.springframework.context.annotation.ClassPathBeanDefinitionScanner}
 * and perform actual scanning and bean definition registration against the container.
 *
 * @author Chris Beams
 * @since 3.1
 * @see MetadataDefinitionReader
 * @see org.springframework.context.annotation.AnnotationMetadataParser
 */
public interface MetadataDefinition {

}
