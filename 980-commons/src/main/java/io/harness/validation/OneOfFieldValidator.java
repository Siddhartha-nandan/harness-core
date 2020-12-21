package io.harness.validation;

import io.harness.reflection.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OneOfFieldValidator implements ConstraintValidator<OneOfField, Object> {
  Set<String> fields = new HashSet<>();
  boolean nullable;
  @Override
  public void initialize(OneOfField oneOfField) {
    Collections.addAll(fields, oneOfField.fields());
    nullable = oneOfField.nullable();
  }

  @Override
  public boolean isValid(Object object, ConstraintValidatorContext context) {
    final int countOfFieldsOfOneOf = fields.stream()
                                         .map(fieldName -> {
                                           final Field field =
                                               ReflectionUtils.getFieldByName(object.getClass(), fieldName);
                                           if (field == null) {
                                             log.error("Field [{}] not found in object", fieldName);
                                           } else {
                                             field.setAccessible(true);
                                             try {
                                               final Object fieldValue = field.get(object);
                                               if (fieldValue != null) {
                                                 return 1;
                                               }
                                             } catch (IllegalAccessException e) {
                                               // Should never reach here.
                                               log.error("Field [{}] not found in object", fieldName);
                                             }
                                           }
                                           return 0;
                                         })
                                         .mapToInt(Integer::valueOf)
                                         .sum();

    return (nullable && (countOfFieldsOfOneOf == 0 || countOfFieldsOfOneOf == 1))
        || (!nullable && countOfFieldsOfOneOf == 1);
  }
}
