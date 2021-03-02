package io.harness.pms.yaml;

import io.harness.beans.CastedField;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import lombok.experimental.UtilityClass;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

@UtilityClass
public class ParameterDocumentFieldMapper {
  public ParameterDocumentField fromParameterField(ParameterField<?> parameterField, CastedField castedField) {
    if (parameterField == null) {
      Class<?> cls = findValueClass(null, castedField);
      if (cls == null) {
        throw new InvalidRequestException("Parameter field is null");
      }
      return ParameterDocumentField.builder()
          .valueDoc(RecastOrchestrationUtils.toDocument(new ParameterFieldValueWrapper<>(null)))
          .valueClass(cls)
          .typeString(cls.isAssignableFrom(String.class))
          .build();
    }
    return ParameterDocumentField.builder()
        .expression(parameterField.isExpression())
        .expressionValue(parameterField.getExpressionValue())
        .valueDoc(RecastOrchestrationUtils.toDocument(new ParameterFieldValueWrapper<>(parameterField.getValue())))
        .valueClass(findValueClass(parameterField, castedField))
        .inputSetValidator(parameterField.getInputSetValidator())
        .typeString(parameterField.isTypeString())
        .jsonResponseField(parameterField.isJsonResponseField())
        .responseField(parameterField.getResponseField())
        .build();
  }

  public ParameterField<?> toParameterField(ParameterDocumentField documentField) {
    if (documentField == null) {
      return null;
    }
    ParameterFieldValueWrapper<?> parameterFieldValueWrapper =
        RecastOrchestrationUtils.fromDocument(documentField.getValueDoc(), ParameterFieldValueWrapper.class);
    return ParameterField.builder()
        .expression(documentField.isExpression())
        .expressionValue(documentField.getExpressionValue())
        .value(parameterFieldValueWrapper == null ? null : parameterFieldValueWrapper.getValue())
        .inputSetValidator(documentField.getInputSetValidator())
        .typeString(documentField.isTypeString())
        .jsonResponseField(documentField.isJsonResponseField())
        .responseField(documentField.getResponseField())
        .build();
  }

  private Class<?> findValueClass(ParameterField<?> parameterField, CastedField castedField) {
    if (parameterField != null && parameterField.getValue() != null) {
      return parameterField.getValue().getClass();
    }
    if (castedField == null || !(castedField.getGenericType() instanceof ParameterizedTypeImpl)) {
      return null;
    }
    return (Class<?>) ((ParameterizedTypeImpl) castedField.getGenericType()).getActualTypeArguments()[0];
  }
}
