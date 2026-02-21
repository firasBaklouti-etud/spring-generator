<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Login</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">
    <div class="container">
        <div class="row justify-content-center mt-5">
            <div class="col-md-4">
                <div class="card shadow">
                    <div class="card-body">
                        <h3 class="card-title text-center mb-4">Login</h3>

                        <div th:if="${r"${param.error}"}" class="alert alert-danger" role="alert">
                            Invalid username or password.
                        </div>
                        <div th:if="${r"${param.logout}"}" class="alert alert-info" role="alert">
                            You have been logged out.
                        </div>
                        <div th:if="${r"${param.registered}"}" class="alert alert-success" role="alert">
                            Registration successful. Please log in.
                        </div>

                        <form th:action="@{/login}" method="post">
                            <div class="mb-3">
                                <label for="username" class="form-label">Username</label>
                                <input type="text" class="form-control" id="username" name="username" required autofocus>
                            </div>
                            <div class="mb-3">
                                <label for="password" class="form-label">Password</label>
                                <input type="password" class="form-control" id="password" name="password" required>
                            </div>
<#if security.rememberMeEnabled?? && security.rememberMeEnabled>
<#if security.rememberMeMode?? && security.rememberMeMode == "CHECKBOX">
                            <div class="mb-3 form-check">
                                <input type="checkbox" class="form-check-input" id="remember-me" name="remember-me">
                                <label class="form-check-label" for="remember-me">Remember me</label>
                            </div>
</#if>
</#if>
                            <button type="submit" class="btn btn-primary w-100">Sign In</button>
                        </form>

<#if !(security.registrationEnabled?? && !security.registrationEnabled)>
                        <div class="text-center mt-3">
                            <a th:href="@{/register}">Don't have an account? Register</a>
                        </div>
</#if>
                    </div>
                </div>
            </div>
        </div>
    </div>
</body>
</html>
