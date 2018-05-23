#!/usr/bin/env bash

# more bash-friendly output for jq
JQ="jq --raw-output --exit-status"

configure_aws_cli(){
	aws --version
	aws configure set default.region us-east-1
	aws configure set default.output json
}

deploy_cluster() {

    family="sportadvisor-api-task-family"

    make_task_def
    register_definition
    if [ $(aws ecs update-service --cluster sportadvisor-api --service sportadvisor-api-service --task-definition $revision | \
                   $JQ '.service.taskDefinition') != $revision ]; then
        echo "Error updating service."
        return 1
    fi

    # wait for older revisions to disappear
    # not really necessary, but nice for demos
    for attempt in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30; do
        echo "attempt $attempt"
        if stale=$(aws ecs describe-services --cluster sportadvisor-api --services sportadvisor-api-service | \
                       $JQ ".services[0].deployments | .[] | select(.taskDefinition != \"$revision\") | .taskDefinition"); then
            echo "Waiting for stale deployments:"
            echo "$stale"
            sleep 5
        else
            echo "Deployed!"
            return 0
        fi
    done
    echo "Service update took too long."
    return 1
}

make_task_def(){
	task_template='[
		{
			"name": "sportadvisor-api",
			"image": "%s.dkr.ecr.us-east-1.amazonaws.com/sportadvisor:latest",
			"essential": true,
			"portMappings": [
                {
                    "containerPort": 5553,
                    "protocol": "tcp"
                }
            ],
			"memory": 400,
			"cpu": 400,
			 "environment": [
                {
                    "name": "JDBC_URL",
                    "value": "jdbc:postgresql://%s"
                },
                {
                    "name": "JDBC_USER",
                    "value": "%s"
                },
                {
                    "name": "JDBC_PASSWORD",
                    "value": "%s"
                },
                {
                    "name":"MAIL_SMTP",
                    "value":"%s"
                },
                {
                    "name":"MAIL_SMPT_PORT",
                    "value":"%s"
                },
                {
                    "name":"MAIL_USER",
                    "value":"%s"
                },
                {
                    "name":"MAIL_PASS",
                    "value":"%s"
                }
            ]
		}
	]'

	task_def=$(printf "$task_template" $AWS_ACCOUNT_ID $RDS_URL $RDS_USER $RDS_PASS $SMTP_ADDRESS $SMTP_PORT $SMTP_USER $SMTP_PASS)
}

push_ecr_image(){
	eval $(aws ecr get-login --region us-east-1 --no-include-email)
	aws ecr batch-delete-image --repository-name sportadvisor --image-ids imageTag=latest
	docker push ${AWS_ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com/sportadvisor:latest
}

register_definition() {

    if revision=$(aws ecs register-task-definition --container-definitions "$task_def" --family $family | $JQ '.taskDefinition.taskDefinitionArn'); then
        echo "Revision: $revision"
    else
        echo "Failed to register task definition"
        return 1
    fi

}

configure_aws_cli
push_ecr_image
deploy_cluster
