package com.sunteco.ipvalidation;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class CoreClient {
    private String basePath = "https://api.ucloud-global.com";
    @Getter
    private Map<String, Object> defaultEmptyParameter = Collections.emptyMap();

    final void init() {
        this.basePath = this.getBasePath();
    }

    //    @Autowired
//    @Qualifier("cephRestTemplate")
    protected RestTemplate restTemplate = new RestTemplate();

    public <T, R> Object post(String path, T body, Map<String, Object> parameters) {
        try {
            if (parameters == null) {
                parameters = defaultEmptyParameter;
            }
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<T> request = new HttpEntity<>(body, httpHeaders);

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(basePath + path);
            for (String para : parameters.keySet()) {
                builder.queryParam(para, String.format("{%s}", para));
            }
            String uriTemplate = builder.encode().toUriString();

            return restTemplate.postForObject(uriTemplate, request, HashMap.class, parameters);
//            return restTemplate.exchange(uriTemplate, HttpMethod.POST, request, Object.class, parameters).getBody();
        } catch (Exception exception) {
            log.error("POST: Exception when call other service, path: {}: {}", path, exception.getMessage());
            throw exception;
        }
    }

    public <T, R> Object post(String path, T body, Map<String, Object> parameters, Class<R> response) {
        try {
            if (parameters == null) {
                parameters = defaultEmptyParameter;
            }
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<T> request = new HttpEntity<>(body, httpHeaders);
            return restTemplate.postForObject(basePath + path, request, response, parameters);
        } catch (Exception exception) {
            log.error("POST: Exception when call other service, path :{} : {}", path, exception.getMessage());
            throw exception;
//            throw new StcException(MessageCode.INTERNAL_ERROR.value(), MessageCode.INTERNAL_ERROR.getContent());
        }
    }

    public <T, R> Object put(String path, T body, Map<String, Object> parameters) {
        try {
            if (parameters == null) {
                parameters = defaultEmptyParameter;
            }
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<T> request = new HttpEntity<>(body, httpHeaders);
            return restTemplate.exchange(basePath + path, HttpMethod.PUT, request, Object.class, parameters).getBody();
        } catch (Exception exception) {
            log.error("PUT: Exception when call other service, path : {} : {}", path, exception.getMessage());
            throw exception;
        }
    }

    public <T, R> Object put(String path, Map<String, Object> parameters) {
        try {
            if (parameters == null) {
                parameters = defaultEmptyParameter;
            }
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<T> request = new HttpEntity<>(httpHeaders);

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(basePath + path);
            for (String para : parameters.keySet()) {
                builder.queryParam(para, String.format("{%s}", para));
            }
            String uriTemplate = builder.encode().toUriString();
            return restTemplate.exchange(uriTemplate, HttpMethod.PUT, request, Object.class, parameters).getBody();
        } catch (Exception exception) {
            log.error("PUT: Exception when call other service, path : {} : {}", path, exception.getMessage());
            throw exception;
        }
    }


    public <T, R> void delete(String path, T body, Map<String, Object> parameters) {
        try {
            if (parameters == null) {
                parameters = defaultEmptyParameter;
            }
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<T> request;
            if (body != null) {
                request = new HttpEntity<>(body, httpHeaders);
            } else {
                request = new HttpEntity<>(httpHeaders);
            }
            restTemplate.exchange(basePath + path, HttpMethod.DELETE, request, Map.class, parameters);
        } catch (Exception exception) {
            log.error("DEL: Exception when call other service, path: {} , {}", path, exception.getMessage());
            throw exception;
//            throw new StcException(MessageCode.INTERNAL_ERROR.value(), MessageCode.INTERNAL_ERROR.getContent());
        }
    }

    public <T, R> Object get(String path, Map<String, Object> parameters) {
        try {
            if (parameters == null) {
                parameters = defaultEmptyParameter;
            }
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<T> request = new HttpEntity<>(httpHeaders);
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(basePath + path);
            for (String para : parameters.keySet()) {
                builder.queryParam(para, String.format("{%s}", para));
            }
            String uriTemplate = builder.encode().toUriString();
            log.info("UIR TEMPLATE: {}", uriTemplate);
            return restTemplate.exchange(uriTemplate, HttpMethod.GET, request, Object.class, parameters).getBody();
        } catch (Exception exception) {
            log.error("GET: Exception when call other service, path : {} : {}", path, exception.getMessage());
            throw exception;
//            throw new StcException(MessageCode.INTERNAL_ERROR.value(), MessageCode.INTERNAL_ERROR.getContent());
        }
    }

    public <T, R> Object get(String path, T body, Map<String, Object> parameters) {
        try {
            if (parameters == null) {
                parameters = defaultEmptyParameter;
            }
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<T> request = new HttpEntity<>(body);
            return restTemplate.exchange(basePath + path, HttpMethod.GET, request, Object.class, parameters).getBody();
        } catch (Exception exception) {
            log.error("GET: Exception when call path: {}: {}", path, exception.getMessage());
            throw exception;
        }
    }


    protected String getBasePath(){
        return "https://api.ucloud-global.com";
    }


}
