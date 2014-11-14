module FakeSQS
  module Actions
    class ChangeMessageVisibility

      def initialize(options = {})
        @server    = options.fetch(:server)
        @queues    = options.fetch(:queues)
        @responder = options.fetch(:responder)
      end

      def call(queue, params)
        visibility = params.fetch("VisibilityTimeout")
        receipt = params.fetch("ReceiptHandle")

        @queues.get(queue).change_message_visibility( receipt, visibility.to_i )
        @responder.call :ChangeMessageVisibility
      end

    end
  end
end